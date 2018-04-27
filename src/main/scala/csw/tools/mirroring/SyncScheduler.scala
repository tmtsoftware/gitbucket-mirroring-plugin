package csw.tools.mirroring

import akka.actor.{ActorSystem, Cancellable, Terminated}
import csw.tools.mirroring.model.Repo
import csw.tools.mirroring.service.{GitService, MirrorService}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble
import scala.util.control.NonFatal

class SyncScheduler(mirrorService: MirrorService) {

  import gitbucket.core.util.ConfigUtil

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
    try {
      for {
        repo   <- repoDetails
        mirror <- mirrorService.findMirror(repo)
        mirrorWithStatus = new GitService(repo, mirror).sync()
        _ <- mirrorService.upsert(repo, mirrorWithStatus)
      } yield {
        println(s"${repo.name} scheduled sync complete")
      }
    } catch {
      case NonFatal(ex) => ex.printStackTrace()
    }
  }

  def start(): Cancellable = {
    ScheduleRunner.start(run)
  }

  def shutdown(): Terminated = {
    ScheduleRunner.shutdown()
  }
}

object ScheduleRunner {
  private val actorSystem = ActorSystem("scheduler")

  def start(f: => Unit): Cancellable = {
    actorSystem.scheduler.schedule(1.minute, 1.minute, () => f _)
  }

  def shutdown(): Terminated = {
    Await.result(actorSystem.terminate(), 10.seconds)
  }

}
