package csw.tools.mirroring.scheduler

import csw.tools.mirroring.model.Repo
import csw.tools.mirroring.rest.HttpClient
import org.quartz.{Job, JobExecutionContext}
import org.slf4j.LoggerFactory

class MirrorSyncJob extends Job {

  private val logger = LoggerFactory.getLogger(classOf[MirrorSyncJob])

  override def execute(context: JobExecutionContext): Unit = {

    val key = context.getJobDetail.getKey

    logger.info(s"Performing mirror sync for Job $key")
    Repo.fromJsonString(key.getName).foreach { repo =>
      HttpClient.sendRequest(repo.owner, repo.name)
    }
  }

}
