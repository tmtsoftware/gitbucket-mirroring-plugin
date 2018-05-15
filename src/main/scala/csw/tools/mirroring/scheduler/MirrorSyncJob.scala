package csw.tools.mirroring.scheduler

import csw.tools.mirroring.model.Repo
import csw.tools.mirroring.rest.HttpGateway
import org.apache.http.impl.client.CloseableHttpClient
import org.quartz.{Job, JobExecutionContext}
import org.slf4j.LoggerFactory

class MirrorSyncJob extends Job {

  private val logger = LoggerFactory.getLogger(classOf[MirrorSyncJob])

  override def execute(context: JobExecutionContext): Unit = {

    val key        = context.getJobDetail.getKey
    val data       = context.getJobDetail.getJobDataMap
    val httpClient = data.get("httpClient").asInstanceOf[CloseableHttpClient]

    logger.info(s"Performing mirror sync for Job $key")
    Repo.fromJsonString(key.getName).foreach { repo =>
      new HttpGateway(httpClient).sendRequest(repo.owner, repo.name)
    }
  }

}
