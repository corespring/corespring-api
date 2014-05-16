package org.corespring.api.v2.actions

import org.corespring.api.v2.services.{ OrgService, TokenService }
import org.corespring.platform.core.controllers.auth.{ UserSession, TokenReader }
import play.api.mvc._
import org.slf4j.LoggerFactory

trait RequestTransformer[A, B <: Request[A]] {
  def apply(rh: Request[A]): Option[B]
}

trait SessionBasedRequestTransformer[A]
  extends RequestTransformer[A, OrgRequest[A]]
  with UserSession {
  def orgService: OrgService
  private lazy val logger = LoggerFactory.getLogger("v2Api.SessionRequestTransformer")
  override def apply(rh: Request[A]): Option[OrgRequest[A]] = {

    import scalaz._
    import scalaz.Scalaz._

    val out: Validation[String, OrgRequest[A]] = for {
      u <- userFromSession(rh).toSuccess("No user in session")
      org <- orgService.org(u.org.orgId).toSuccess(s"No org for user ${u}")
      dc <- orgService.defaultCollection(org).toSuccess(s"No default collection for org ${org.id}")
    } yield {
      OrgRequest[A](rh, org.id, dc)
    }

    out match {
      case Success(or) => Some(or)
      case Failure(msg) => {
        logger.trace(msg)
        None
      }
    }
  }
}

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

