package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.noUserSession
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc._
import scalaz.{ Failure, Success, Validation }

trait UserSessionOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with UserSession {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "UserSessionIdentity")

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    userFromSession(rh).map { u =>
      Success(u.org.orgId)
    }.getOrElse(Failure(noUserSession(rh)))
  }

}

