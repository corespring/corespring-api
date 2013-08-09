package org.corespring.player.accessControl.auth.requests

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.AccessToken
import play.api.mvc.Request

trait TokenizedRequestBuilder {

  def buildTokenizedRequest[A](orgId: ObjectId)(implicit r: Request[A]): Option[TokenizedRequest[A]] =
    AccessToken.getTokenForOrgById(orgId).map(t => TokenizedRequest(t.tokenId, r))
}
