package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.errors.Errors.{ generalError, identificationFailed }
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait TokenOrgIdentity[B]
  extends OrgRequestIdentity[B]
  with TokenReader {

  def tokenService: TokenService

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    def onToken(token: String) = tokenService.orgForToken(token).map { o =>
      Success(o.id)
    }.getOrElse(Failure(generalError(s"Cant find org for token $token")))
    getToken[String](rh, "Invalid token", "No token").fold(e => Failure(identificationFailed(rh, e)), onToken)
  }

  override def toString = s"[TokenOrgIdentity]"

}
