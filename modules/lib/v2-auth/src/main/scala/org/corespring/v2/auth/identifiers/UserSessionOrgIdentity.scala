package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.platform.core.models.Organization
import org.corespring.v2.errors.Errors.{cantFindOrgWithId, noUserSession}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc._

import scalaz.Scalaz._
import scalaz.Validation

trait UserSessionOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with UserSession {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "UserSessionIdentity")

  override def headerToOrg(rh: RequestHeader): Validation[V2Error, Organization] = for {
    u <- userFromSession(rh).toSuccess(noUserSession(rh))
    org <- orgService.org(u.org.orgId).toSuccess(cantFindOrgWithId(u.org.orgId))
  } yield org

}

