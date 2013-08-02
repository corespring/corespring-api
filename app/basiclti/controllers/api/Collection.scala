package basiclti.controllers.api


import common.controllers.SimpleJsRoutes
import models.auth.AccessToken
import org.bson.types.ObjectId
import play.api.mvc._
import scala.Some
import controllers.auth.requests.TokenizedRequest
import basiclti.accessControl.auth.ValidateQuizIdAndOrgId

class Collection(auth: ValidateQuizIdAndOrgId[TokenizedRequest[AnyContent]]) extends Controller with SimpleJsRoutes {

  import api.v1.{CollectionApi => Api}

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction((quizId, orgId) => true) { r : TokenizedRequest[AnyContent] =>
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
  }
)
