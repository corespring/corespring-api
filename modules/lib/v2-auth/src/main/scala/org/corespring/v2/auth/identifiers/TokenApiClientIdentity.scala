package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.Errors.{noApiClientForToken, noToken, invalidToken}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.{Failure, Success, Validation}

trait TokenApiClientIdentity[B]
  extends ApiClientRequestIdentity[B]
  with TokenReader {

  def tokenService: TokenService

  override lazy val logger = V2LoggerFactory.getLogger("auth", "TokenApiClientIdentity")

  override def headerToApiClient(rh: RequestHeader): Validation[V2Error, ApiClient] = {
    def onToken(token: String) = tokenService.apiClientForToken(token)(rh).map { o =>
      Success(o)
    }.getOrElse(Failure(noApiClientForToken(rh)))

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

}
