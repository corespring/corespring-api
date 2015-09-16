package org.corespring.it.helpers

import grizzled.slf4j.Logger
import org.corespring.common.encryption.AESCrypto
import org.corespring.models.auth.ApiClient
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.slf4j.LoggerFactory
import play.api.http.Writeable
import play.api.libs.json.Json
import play.api.mvc._
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.Future

trait IntegrationHelpers extends PlaySpecification {

  protected val logger = Logger("it.helpers")

  def makeRequest(call: Call, c: Seq[Cookie]): Request[AnyContentAsEmpty.type] = {
    val req = FakeRequest(call.method, call.url)
    req.withCookies(c: _*)
  }

  def urlWithEncryptedOptions(call: Call, apiClient: ApiClient, options: PlayerAccessSettings = PlayerAccessSettings.ANYTHING) = {
    val options = Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))
    val encrypted = AESCrypto.encrypt(options, apiClient.clientSecret)
    s"${call.url}?apiClient=${apiClient.clientId}&options=$encrypted"
  }

  def getResultFor[T](request: Request[T])(implicit writable: Writeable[T]): Option[Future[SimpleResult]] = {

    logger.debug(s"[getResultFor] ${request.method}, ${request.path}")

    val result: Option[Future[SimpleResult]] = route(request)

    logger.debug(s"[getResultFor] -> ${request.method}, ${request.path}")

    result.foreach {
      r =>
        logger.debug(s"[getResultFor] status: ${status(r)}")
    }

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
