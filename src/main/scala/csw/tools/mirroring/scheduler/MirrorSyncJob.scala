package csw.tools.mirroring.scheduler

import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.service.{GitService, MirrorService}
import org.quartz.{Job, JobExecutionContext, JobKey}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.DurationDouble

class MirrorSyncJob extends Job {

  private val logger = LoggerFactory.getLogger(classOf[MirrorSyncJob])

  import org.quartz.JobDataMap

  override def execute(context: JobExecutionContext): Unit = {

    val data: JobDataMap             = context.getJobDetail.getJobDataMap
    val mirrorService: MirrorService = data.get("mirrorService").asInstanceOf[MirrorService]
    val repo: Repo                   = data.get("repo").asInstanceOf[Repo]
    val mirror: Mirror               = data.get("mirror").asInstanceOf[Mirror]
    val key: JobKey                  = context.getJobDetail.getKey

    def syncMirror(repo: Repo, mirror: Mirror): Unit = {
      logger.info(s"Performing mirror sync for Job $key")
      mirrorService.upsert(repo, new GitService(repo, mirror).sync())
    }

    if (mirror.enabled) {
      logger.info(s"Mirror sync is enabled")
      val delayMinute = mirror.syncInterval.minutes
      logger.info(s"Scheduling mirror sync after every $delayMinute")
      syncMirror(repo, mirror)
    } else logger.info(s"Mirror sync is disabled for repo = $repo")
  }

}
