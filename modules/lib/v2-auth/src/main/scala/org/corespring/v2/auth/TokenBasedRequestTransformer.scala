package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.TokenReader
import org.corespring.v2.auth.services.TokenService
import play.api.mvc.RequestHeader

trait TokenBasedRequestTransformer[B]
  extends WithOrgTransformer[B]
  with TokenReader {

  def tokenService: TokenService

  override def getOrgId(rh: RequestHeader): Either[String, ObjectId] = {
    def onToken(token: String) = tokenService.orgForToken(token).map { o => Right(o.id) }.getOrElse(Left("No org"))
    getToken[String](rh, "Invalid token", "No token").fold(Left(_), onToken)
  }

}
