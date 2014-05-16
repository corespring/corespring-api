package org.corespring.api.v2.actions

import org.corespring.api.v2.services.OrgService
import org.corespring.platform.core.controllers.auth.UserSession
import org.slf4j.LoggerFactory
import play.api.mvc._

trait SessionBasedRequestTransformer[A]
  extends RequestTransformer[A, OrgRequest[A]]
  with UserSession {
  def orgService: OrgService

  private lazy val logger = LoggerFactory.getLogger("v2Api.SessionRequestTransformer")

  override def apply(rh: Request[A]): Option[OrgRequest[A]] = {

    import scalaz.Scalaz._
    import scalaz._

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

