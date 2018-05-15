package csw.tools.mirroring.rest

import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

object HttpClient {

  private val httpClient: CloseableHttpClient = HttpClients.createDefault()

  private def httpPut(owner: String, repositoryName: String): HttpPut = {
    new HttpPut(s"http://localhost:4000/api/v3/repos/$owner/$repositoryName/mirror/status")
  }

  def sendRequest(owner: String, repositoryName: String): Unit = {
    var response: CloseableHttpResponse = null
    try {
      response = httpClient.execute(httpPut(owner, repositoryName))
    } catch {
      case exception: Throwable => exception.printStackTrace()
    } finally {
      response.close()
    }
  }

  def shutdown(): Unit = httpClient.close()
}
