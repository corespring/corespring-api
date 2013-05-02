package controllers.auth.requests

import org.bson.types.ObjectId
import models.auth.AccessToken
import play.api.mvc.Request

trait TokenizedRequestBuilder {

  def buildTokenizedRequest[A](orgId: ObjectId)(implicit r: Request[A]): Option[TokenizedRequest[A]] =
    AccessToken.getTokenForOrgById(orgId).map(t => TokenizedRequest(t.tokenId, r))
}
