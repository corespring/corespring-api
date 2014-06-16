package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.v2.auth.services.TokenService
import play.api.mvc.RequestHeader
import scalaz.{Failure, Success, Validation}

trait TokenBasedRequestTransformer[B]
  extends WithOrgTransformer[B]
  with TokenReader {

  def tokenService: TokenService

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    def onToken(token: String) = tokenService.orgForToken(token).map { o => Success(o.id) }.getOrElse(Failure("No org"))
    getToken[String](rh, "Invalid token", "No token").fold(Failure(_), onToken)
  }

}
