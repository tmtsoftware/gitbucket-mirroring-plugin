package csw.tools.mirroring

import akka.actor.{ActorSystem, Cancellable, Terminated}
import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.service.{GitService, MirrorService}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

class SyncScheduler(mirrorService: MirrorService) {

  import gitbucket.core.util.ConfigUtil

  private val actorSystem = ActorSystem("scheduler")

  private var scheduleSync: Option[Cancellable] = None

  private var min: Option[String] = None
  private var hrs: Option[String] = None

  private val logger = LoggerFactory.getLogger(classOf[SyncScheduler])

  private val repoDetails = for {
    repositoryOwner <- ConfigUtil.getEnvironmentVariable("MIRROR_REPO_OWNER").asInstanceOf[Option[String]]
    repositoryName  <- ConfigUtil.getEnvironmentVariable("MIRROR_REPO_NAME").asInstanceOf[Option[String]]
  } yield {
    Repo(repositoryOwner, repositoryName)
  }

  logger.info("Looking for repo details to schedule mirror sync . . .")
  repoDetails.fold {
    logger.warn("No config found for scheduling mirror sync")
    logger.warn("Make sure value for GITBUCKET_MIRROR_REPO_OWNER, GITBUCKET_MIRROR_REPO_NAME are set in env variables")
  } { repo =>
    logger.info(s"Found repo details for scheduling mirror sync : $repo")
  }

  def run(): Unit = {
    for {
      repo   <- repoDetails
      mirror <- mirrorService.findMirror(repo)
    } yield {

      if (mirror.enabled) {
        if (scheduleSync.isEmpty || !min.contains(mirror.minutes) || !hrs.contains(mirror.hours)) {
          scheduleSync.foreach(_.cancel())
          scheduleSync = Option(
            actorSystem.scheduler.schedule(
              1.minute,
              (mirror.hours.toInt * 60 + mirror.minutes.toInt).minute,
              () => {
                logger.info(s"Performing mirror sync")
                mirrorService.upsert(repo, new GitService(repo, mirror).sync())
              }
            )
          )
          min = Some(mirror.minutes)
          hrs = Some(mirror.hours)
        }
      } else {
        logger.info(s"Mirror sync schedule is disabled")
        scheduleSync.foreach(_.cancel())
        scheduleSync = None
        min = None
        hrs = None
      }
    }
  }

  def start(): Cancellable = {
    actorSystem.scheduler.schedule(0.minute, 1.minute, () => run())
  }

  def shutdown(): Terminated = {
    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
