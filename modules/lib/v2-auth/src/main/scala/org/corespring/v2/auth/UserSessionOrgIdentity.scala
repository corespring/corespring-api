package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.v2.errors.Errors.identificationFailed
import org.corespring.v2.errors.V2Error
import play.api.mvc._

import scalaz.{ Failure, Success, Validation }

trait UserSessionOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with UserSession {

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    userFromSession(rh).map { u =>
      Success(u.org.orgId)
    }.getOrElse(Failure(identificationFailed(rh, "userSession")))
  }

  override def toString = "[UserSession]"
}

