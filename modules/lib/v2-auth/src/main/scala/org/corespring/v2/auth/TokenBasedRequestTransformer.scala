package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.TokenService
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

trait TokenBasedRequestTransformer[B]
  extends CoreTransformer[B]
  with TokenReader {

  private lazy val logger = LoggerFactory.getLogger("v2Api.TokenRequestTransformer")

  def tokenService: TokenService

  override def apply(rh: RequestHeader): Option[B] = {

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
