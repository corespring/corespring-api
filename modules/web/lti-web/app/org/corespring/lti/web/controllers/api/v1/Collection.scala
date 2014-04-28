package org.corespring.lti.web.controllers.api.v1

import org.bson.types.ObjectId
import org.corespring.lti.web.accessControl.auth.ValidateAssessmentIdAndOrgId
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.web.common.controllers.SimpleJsRoutes
import play.api.mvc._
import scala.Some

class Collection(auth: ValidateAssessmentIdAndOrgId[TokenizedRequest[AnyContent]]) extends Controller with SimpleJsRoutes {

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction((assessmentId: String, orgId: String) => true) { r: TokenizedRequest[AnyContent] =>
      import org.corespring.api.v1.{ CollectionApi => Api }
      Api.list(q, f, c, sk, l, sort)(r)
    }
}

object Collection extends Collection(
  new ValidateAssessmentIdAndOrgId[TokenizedRequest[AnyContent]] {
    def makeRequest(orgId: ObjectId, request: Request[AnyContent]) = {
      AccessToken.getTokenForOrgById(orgId) match {
        case Some(token) => Some(new TokenizedRequest[AnyContent](token.tokenId, request))
        case _ => None
      }
    }
  })
