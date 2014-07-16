package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.{ generalError, invalidToken, noToken }
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait TokenOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with TokenReader {

  def tokenService: TokenService

  override lazy val logger = V2LoggerFactory.getLogger("auth", "TokenOrgIdentity")

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    def onToken(token: String) = tokenService.orgForToken(token).map { o =>
      Success(o.id)
    }.getOrElse(Failure(generalError(s"Can't find org for token $token")))

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

}
