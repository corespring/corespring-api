package org.corespring.v2.auth.identifiers

import org.corespring.models.{ User, Organization }
import org.corespring.v2.errors.Errors.{ cantFindOrgWithId, noUserSession }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.web.user.UserFromRequest
import play.api.mvc._

import scalaz.Scalaz._
import scalaz.Validation

trait UserSessionOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with UserFromRequest {

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = None

  override lazy val logger = V2LoggerFactory.getLogger("auth", "UserSessionIdentity")

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = for {
    u <- userFromSession(rh).toSuccess(noUserSession(rh))
    org <- orgService.findOneById(u.org.orgId).toSuccess(cantFindOrgWithId(u.org.orgId))
  } yield (org, Some(u))

}

