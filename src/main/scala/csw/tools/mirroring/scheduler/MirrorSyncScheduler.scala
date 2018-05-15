package csw.tools.mirroring.scheduler

import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.service.MirrorService
import org.apache.http.impl.client.CloseableHttpClient
import org.quartz.DateBuilder.IntervalUnit._
import org.quartz.DateBuilder._
import org.quartz.JobBuilder.newJob
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz._
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

class MirrorSyncScheduler(mirrorService: MirrorService, httpClient: CloseableHttpClient) {

  import org.quartz.impl.StdSchedulerFactory

  private val logger    = LoggerFactory.getLogger(classOf[MirrorSyncScheduler])
  private val scheduler = new StdSchedulerFactory().getScheduler()

  def start(): Unit    = scheduler.start()
  def shutdown(): Unit = scheduler.shutdown()

  def scheduleAll(): Unit = mirrorService.getAllMirrors.foreach {
    case (repo, mirror) => upsertJobWithTrigger(repo, mirror)
  }

  private def buildJob(mirror: Mirror, repoKey: String): JobDetail = {
    newJob(classOf[MirrorSyncJob])
      .withIdentity(repoKey)
      .usingJobData(new JobDataMap(Map("httpClient" -> httpClient).asJava))
      .build
  }

  private def buildTrigger(mirror: Mirror, repoKey: String): SimpleTrigger = {
    newTrigger
      .withIdentity(repoKey)
      .startAt(futureDate(mirror.syncIntervalInMinutes, MINUTE))
      .withSchedule(
        simpleSchedule()
          .withIntervalInMinutes(mirror.syncIntervalInMinutes)
          .repeatForever()
      )
      .build
  }

  def upsertJobWithTrigger(repo: Repo, mirror: Mirror): Unit = {
    val repoKey = Repo.toJsonString(repo)

    if (mirror.enabled) {
      logger.info(s"Updating scheduler for repoKey = $repoKey")
      logger.info(s"Sync job would fire after every ${mirror.syncIntervalInMinutes} minute(s) for repoKey = $repoKey")
      val newJob     = buildJob(mirror, repoKey)
      val newTrigger = buildTrigger(mirror, repoKey)
      scheduler.scheduleJob(newJob, Set(newTrigger).asJava, true)
    } else {
      logger.info(s"Mirror sync is disabled for repoKey = $repoKey")
      deleteMirrorSyncJob(repoKey)
    }
  }

  def deleteMirrorSyncJob(repoKey: String): Unit = {
    Option(scheduler.getJobDetail(new JobKey(repoKey))).foreach { job =>
      logger.info(s"Removing mirror sync job for repoKey = $repoKey")
      scheduler.deleteJob(job.getKey)
    }
  }
}
