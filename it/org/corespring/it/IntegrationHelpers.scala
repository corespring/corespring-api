package org.corespring.it

import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc.{ SimpleResult, Request, Call }
import play.api.test.PlaySpecification
import scala.concurrent.Future
import org.slf4j.LoggerFactory

trait IntegrationHelpers extends PlaySpecification {

  val logger = LoggerFactory.getLogger("it.spec.helper")

  def getEncryptedOptions(call: Call, apiClient: ApiClient, options: PlayerOptions = PlayerOptions.ANYTHING) = {
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
