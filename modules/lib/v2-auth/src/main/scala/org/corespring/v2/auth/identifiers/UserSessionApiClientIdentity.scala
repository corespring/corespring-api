package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.errors.Errors.{cantFindOrgWithId, noUserSession}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.Validation

trait UserSessionApiClientIdentity[B]
  extends ApiClientRequestIdentity[B]
  with UserSession {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "UserSessionApiClientIdentity")

  override def headerToApiClient(rh: RequestHeader): Validation[V2Error, ApiClient] = for {
    u <- userFromSession(rh).toSuccess(noUserSession(rh))
    apiClient <- apiClientService.findOneByOrgId(u.org.orgId).toSuccess(cantFindOrgWithId(u.org.orgId))
  } yield apiClient

}
