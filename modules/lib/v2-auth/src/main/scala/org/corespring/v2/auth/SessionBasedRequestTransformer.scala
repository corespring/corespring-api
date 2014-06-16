package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import play.api.mvc._

import scalaz.{Failure, Success, Validation}

trait SessionBasedRequestTransformer[B]
  extends WithServiceOrgTransformer[B]
  with UserSession {

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    userFromSession(rh).map{ u =>
      Success(u.org.orgId)
    }.getOrElse(Failure("No user session found"))
  }
}

