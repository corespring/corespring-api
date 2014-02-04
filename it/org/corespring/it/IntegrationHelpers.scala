package org.corespring.it

import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.slf4j.LoggerFactory
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{ FakeRequest, PlaySpecification }
import scala.concurrent.Future

trait IntegrationHelpers extends PlaySpecification {

  val logger = LoggerFactory.getLogger("it.spec.helper")

  def makeRequest(call: Call, c: Cookies): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c.toSeq: _*)
  }

  def urlWithEncryptedOptions(call: Call, apiClient: ApiClient, options: PlayerOptions = PlayerOptions.ANYTHING) = {
    val options = Json.stringify(Json.toJson(PlayerOptions.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    s"${call.url}?apiClient=${apiClient.clientId}&options=$encrypted"
  }

  def getResultFor[T](request: Request[T])(implicit writable: Writeable[T]): Option[Future[SimpleResult]] = {
    val result: Option[Future[SimpleResult]] = route(request)
    result.filter {
      r =>
        {
          val s = status(r)
          def okStatus = s == SEE_OTHER || s == OK

          if (!okStatus) {
            logger.warn(s"${request.path} status: $s")
            logger.warn(s"${request.path} content: ${contentAsString(r)}")
          }
          okStatus
        }
    }
  }
}
