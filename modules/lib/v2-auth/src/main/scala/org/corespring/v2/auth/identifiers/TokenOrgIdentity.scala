package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.platform.core.models.{ User, Organization }
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.Errors.{ invalidToken, noOrgForToken, noToken }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }

trait TokenOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with TokenReader {

  override val name = "access-token-in-query-string"

  def tokenService: TokenService

  override lazy val logger = V2LoggerFactory.getLogger("auth", "TokenOrgIdentity")

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = {
    def onToken(token: String) = tokenService.orgForToken(token)(rh).map { o =>
      Success(o, None)
    }.getOrElse(Failure(noOrgForToken(rh)))

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = None
}
