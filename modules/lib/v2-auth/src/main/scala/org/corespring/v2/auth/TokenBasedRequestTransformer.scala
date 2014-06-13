package org.corespring.v2.auth

import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.platform.core.models.Organization
import org.slf4j.LoggerFactory
import play.api.mvc.{ Request, RequestHeader }

trait TokenBasedRequestTransformer[A, B]
  extends RequestTransformer[A, B]
  with TokenReader {

  private lazy val logger = LoggerFactory.getLogger("v2Api.TokenRequestTransformer")

  def data(rh: RequestHeader, org: Organization): B
  def tokenService: TokenService

  override def apply(rh: Request[A]): Option[B] = {

    def onToken(token: String) = {
      val result = for {
        org <- tokenService.orgForToken(token)
      } yield {
        data(rh, org)
      }
      result
    }

    def onError(msg: String) = {
      logger.trace(msg)
      None
    }

    logger.trace(s"getToken")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }
}
