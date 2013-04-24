package player.controllers

import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc.Results._
import play.api.mvc._
import player.controllers.auth._
import player.models.TokenizedRequest
import player.rendering.PlayerCookieReader

class Collection(auth: PlayerAuthenticate) extends Controller with SimpleJsRoutes {

  import api.v1.{CollectionApi => Api}

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.OrgAction(RequestedAccess()) {
      Api.list(q, f, c, sk, l, sort)
    }

  def jsRoutes = Action {
    implicit request =>
      import routes.javascript.{Collection => JsCollection}
      val jsRoutes = List(
        JsCollection.list
      )
      Ok(createSimpleRoutes("PlayerCollectionRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

/** For the collection list - we don't care about modes - we only care if the RenderOptions contains a wildcard for collectionId */
object CheckSessionForCollectionWildcard extends PlayerAuthenticate with PlayerCookieReader with TokenizedRequestBuilder {

  def OrgAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def OrgAction(p: BodyParser[AnyContent])(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action {
      implicit request =>
        val result = for {
          options <- renderOptions(request)
          orgId <- orgIdFromCookie(request)
          if (options.collectionId == "*")
        } yield block(buildTokenizedRequest(new ObjectId(orgId)))

        result.getOrElse(BadRequest("Invalid options or org id"))
    }

}

object Collection extends Collection(CheckSessionForCollectionWildcard)
