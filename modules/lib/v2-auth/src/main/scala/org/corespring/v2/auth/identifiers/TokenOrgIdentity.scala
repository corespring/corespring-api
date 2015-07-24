package org.corespring.v2.auth.identifiers

import org.corespring.models.{ User, Organization }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.AccessTokenService
import org.corespring.v2.errors.Errors.{ invalidToken, noOrgForToken, noToken }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import org.corespring.web.token.TokenReader
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }

abstract class TokenOrgIdentity[B](
  tokenService: AccessTokenService,
  val orgService: OrganizationService)
  extends OrgRequestIdentity[B]
  with TokenReader {

  override lazy val logger = Logger(classOf[TokenOrgIdentity[B]])

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = {
    def onToken(token: String) = tokenService.orgForToken(token).map { o =>
      Success(o, None)
    }.getOrElse(Failure(noOrgForToken(rh)))

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = None
}
