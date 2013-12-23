package basiclti.controllers.api

import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc._
import scala.Some
import basiclti.accessControl.auth.ValidateQuizIdAndOrgId
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.player.accessControl.auth.requests.TokenizedRequest

class Collection(auth: ValidateQuizIdAndOrgId[TokenizedRequest[AnyContent]]) extends Controller with SimpleJsRoutes {

  import api.v1.{ CollectionApi => Api }

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction((quizId : String, orgId : String) => true) { r: TokenizedRequest[AnyContent] =>
      Api.list(q, f, c, sk, l, sort)(r)
    }
}

object Collection extends Collection(
  new ValidateQuizIdAndOrgId[TokenizedRequest[AnyContent]] {
    def makeRequest(orgId: ObjectId, request: Request[AnyContent]) = {
      AccessToken.getTokenForOrgById(orgId) match {
        case Some(token) => Some(new TokenizedRequest[AnyContent](token.tokenId, request))
        case _ => None
      }
    }
  })
