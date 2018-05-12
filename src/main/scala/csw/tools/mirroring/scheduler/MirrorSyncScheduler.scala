package csw.tools.mirroring.scheduler

import csw.tools.mirroring.model.{Mirror, Repo}
import csw.tools.mirroring.service.MirrorService
import org.quartz.JobBuilder.newJob
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.{JobDataMap, JobDetail, Scheduler, SimpleTrigger}

import scala.collection.JavaConverters._

class MirrorSyncScheduler(mirrorService: MirrorService) {

  import org.quartz.impl.StdSchedulerFactory

  private val sf                   = new StdSchedulerFactory
  private val scheduler: Scheduler = sf.getScheduler()

  def getScheduler: Scheduler = scheduler

  for {
    (repo, mirror) <- mirrorService.getAllMirrors
  } yield {
    if (mirror.enabled) {
      upsertJobWithTrigger(repo, mirror)
    }
  }

  private def buildJob(repo: Repo, mirror: Mirror, repoKey: String): JobDetail = {
    val job = newJob(classOf[MirrorSyncJob])
      .withIdentity(repoKey)
      .storeDurably()
      .usingJobData(
        new JobDataMap(
          Map(
            "repo"          -> repo,
            "mirror"        -> mirror,
            "mirrorService" -> mirrorService
          ).asJava
        )
      )
      .build
    job
  }

  private def buildTrigger(mirror: Mirror, name: String): SimpleTrigger = {
    val trigger = newTrigger
      .withIdentity(name)
      .startNow()
      .withSchedule(
        simpleSchedule()
          .withIntervalInMinutes(mirror.syncInterval)
          .repeatForever()
      )
      .build
    trigger
  }

  def upsertJobWithTrigger(repo: Repo, mirror: Mirror): Unit = {
    if (mirror.enabled) {
      val repoKey    = mirrorService.makeKey(repo)
      val newJob     = buildJob(repo, mirror, repoKey)
      val newTrigger = buildTrigger(mirror, repoKey)
      scheduler.scheduleJob(newJob, Set(newTrigger).asJava, true)
    }
  }
}
