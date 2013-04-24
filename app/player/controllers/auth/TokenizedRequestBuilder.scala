package player.controllers.auth

import models.auth.AccessToken
import org.bson.types.ObjectId
import play.api.mvc.Request
import player.models.TokenizedRequest


trait TokenizedRequestBuilder {

  def buildTokenizedRequest[A](orgId: ObjectId)(implicit request: Request[A]): TokenizedRequest[A] = {
    AccessToken.getTokenForOrgById(orgId) match {
      case Some(token) => TokenizedRequest(token.tokenId, request)
      case _ => TokenizedRequest("unknown_user", request)
    }
  }
}

