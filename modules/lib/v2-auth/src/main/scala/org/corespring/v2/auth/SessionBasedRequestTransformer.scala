package org.corespring.v2.auth

import org.corespring.platform.core.controllers.auth.UserSession
import org.slf4j.LoggerFactory
import play.api.mvc._

trait SessionBasedRequestTransformer[B]
  extends CoreTransformer[B]
  with UserSession {

  private lazy val logger = LoggerFactory.getLogger("v2Api.SessionRequestTransformer")

  override def apply(rh: RequestHeader): Option[B] = {

    import scalaz.Scalaz._
    import scalaz._

    val out: Validation[String, B] = for {
      u <- userFromSession(rh).toSuccess("No user in session")
      org <- orgService.org(u.org.orgId).toSuccess(s"No org for user ${u}")
      dc <- orgService.defaultCollection(org).toSuccess(s"No default collection for org ${org.id}")
    } yield {
      data(rh, org) //OrgRequest[A](rh, org.id, dc)
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

