package csw.tools.mirroring

import akka.actor.{ActorSystem, Cancellable, Terminated}
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global

class SyncScheduler {
  private val httpPut                         = new HttpPut("http://localhost:8080/api/v3/repos/root/test/mirror/status")
  private val httpClient: CloseableHttpClient = HttpClients.createDefault()
  private val actorSystem                     = ActorSystem("scheduler")

  def run(): Unit = {
    var response: CloseableHttpResponse = null
    try {
      response = httpClient.execute(httpPut)
      println(response.toString)
    } catch {
      case NonFatal(ex) => ex.printStackTrace()
    } finally {
      response.close()
    }
  }

  def start(): Cancellable = {
    actorSystem.scheduler.schedule(1.minute, 1.minute, () => run())
  }

  def shutdown(): Terminated = {
    httpClient.close()
    Await.result(actorSystem.terminate(), 10.seconds)
  }
}
