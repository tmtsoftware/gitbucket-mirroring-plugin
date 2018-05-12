package csw.tools.mirroring

import akka.actor.{ActorSystem, Cancellable, Terminated}
import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.service.{GitService, MirrorService}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

class SyncScheduler(mirrorService: MirrorService) {

  private val actorSystem = ActorSystem("scheduler")

  private val logger = LoggerFactory.getLogger(classOf[SyncScheduler])

  private val scheduleMap: mutable.Map[String, Cancellable] = mutable.Map[String, Cancellable]()

  def run(): Unit = {
    for {
      (repo, mirror) <- mirrorService.getAllMirrors
    } yield {

      val repoKey = mirrorService.makeKey(repo)

      if (scheduleMap.get(repoKey).isEmpty) {
        if (mirror.enabled) {
          logger.info(s"Mirror sync is enabled")
          val delayMinute = mirror.syncInterval.minutes
          logger.info(s"Scheduling mirror sync after every $delayMinute")
          val scheduledCancellable = actorSystem.scheduler.schedule(0.seconds, delayMinute, () => syncMirror(repo, mirror))

          scheduleMap += repoKey -> scheduledCancellable

        } else {
          logger.info(s"Mirror sync is disabled for repo = $repo")
        }
      }
    }
  }

  def syncMirror(repo: Repo, mirror: Mirror): Option[Mirror] = {
    logger.info(s"Performing mirror sync")
    mirrorService.upsert(repo, new GitService(repo, mirror).sync())
  }

  def updateSchedule(repo: Repo, mirror: Mirror): Unit = {
    val repoKey = mirrorService.makeKey(repo)
    scheduleMap.get(repoKey).foreach(_.cancel())
    if (mirror.enabled)
      scheduleMap +=
        repoKey -> actorSystem.scheduler.schedule(0.seconds, mirror.syncInterval.minutes, () => syncMirror(repo, mirror))
  }

  def start(): Cancellable = {
    actorSystem.scheduler.schedule(0.seconds, 10.seconds, () => run())
  }

  def shutdown(): Terminated = {
    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
