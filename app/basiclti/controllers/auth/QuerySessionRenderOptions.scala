package basiclti.controllers.auth

import controllers.auth.RenderOptions
import org.bson.types.ObjectId
import player.controllers.auth.{TokenizedRequestBuilder, AuthenticateAndUseToken}
import player.models.TokenizedRequest
import player.rendering.PlayerCookieReader


/** An implementation of Authenticate where we check the user session rendering options */
object QuerySessionRenderOptions extends AuthenticateAndUseToken[RenderOptions => Boolean] with PlayerCookieReader with TokenizedRequestBuilder {

  type RenderOptionQuery = (RenderOptions => Boolean)

  import play.api.mvc.Results.BadRequest
  import play.api.mvc.{BodyParser, Result, Action, AnyContent}

  def OrgAction(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def OrgAction(p: BodyParser[AnyContent])(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action {
      implicit request =>
        val result = for {
          options <- renderOptions(request)
          orgId <- orgIdFromCookie(request)
          if ( query(options) )
        } yield block(buildTokenizedRequest(new ObjectId(orgId)))
        result.getOrElse(BadRequest("Invalid options or org id"))
    }
}
