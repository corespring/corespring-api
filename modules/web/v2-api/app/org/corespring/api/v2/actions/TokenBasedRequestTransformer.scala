package org.corespring.api.v2.actions

import org.corespring.api.v2.services.{OrgService, TokenService}
import org.corespring.platform.core.controllers.auth.TokenReader
import org.slf4j.LoggerFactory
import play.api.mvc.Request

trait TokenBasedRequestTransformer[A]
  extends RequestTransformer[A, OrgRequest[A]]
  with TokenReader {

  private lazy val logger = LoggerFactory.getLogger("v2Api.TokenRequestTransformer")

  def tokenService: TokenService

  def orgService: OrgService

  override def apply(rh: Request[A]): Option[OrgRequest[A]] = {

    def onToken(token: String) = {
      val result = for {
        org <- tokenService.orgForToken(token)
        dc <- orgService.defaultCollection(org)
      } yield {
        logger.trace(s"return org request for org: ${org.id}")
        OrgRequest[A](rh, org.id, dc)
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
