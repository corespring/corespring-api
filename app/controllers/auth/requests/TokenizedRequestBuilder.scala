package controllers.auth.requests

import org.bson.types.ObjectId
import play.api.mvc.Request
import org.corespring.platform.core.models.auth.AccessToken

trait TokenizedRequestBuilder {

  def buildTokenizedRequest[A](orgId: ObjectId)(implicit r: Request[A]): Option[TokenizedRequest[A]] =
    AccessToken.getTokenForOrgById(orgId).map(t => TokenizedRequest(t.tokenId, r))
}
