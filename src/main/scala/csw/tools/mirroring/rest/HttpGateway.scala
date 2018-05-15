package csw.tools.mirroring.rest

import org.apache.http.client.methods.{CloseableHttpResponse, HttpPut}
import org.apache.http.impl.client.CloseableHttpClient

import scala.util.control.NonFatal

class HttpGateway(httpClient: CloseableHttpClient) {

  def sendRequest(owner: String, repositoryName: String): Unit = {
    var response: CloseableHttpResponse = null
    try {
      val uri = s"http://localhost:4000/api/v3/repos/$owner/$repositoryName/mirror/status"
      val put = new HttpPut(uri)
      response = httpClient.execute(put)
    } catch {
      case NonFatal(ex) => ex.printStackTrace()
    } finally {
      Option(response).foreach(_.close())
    }
  }

}
