package basiclti.controllers.api


import basiclti.controllers.auth.ValidateQuizIdAndOrgId
import common.controllers.SimpleJsRoutes
import models.auth.AccessToken
import org.bson.types.ObjectId
import play.api.mvc._
import player.models.TokenizedRequest
import scala.Some

class Collection(auth: ValidateQuizIdAndOrgId[TokenizedRequest[AnyContent]]) extends Controller with SimpleJsRoutes {

  import api.v1.{CollectionApi => Api}

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.OrgAction((quizId, orgId) => true) {
      Api.list(q, f, c, sk, l, sort)
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
