package basiclti.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.{ TokenizedRequestBuilder, TokenizedRequest }
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import org.corespring.player.accessControl.models.RenderOptions

/** An implementation of ActionBuilder where we check the user session rendering options */
object QuerySessionRenderOptions extends TokenizedRequestActionBuilder[RenderOptions => Boolean] with PlayerCookieReader with TokenizedRequestBuilder {

  type RenderOptionQuery = (RenderOptions => Boolean)

  import play.api.mvc.Results.BadRequest
  import play.api.mvc.{ BodyParser, Result, Action, AnyContent }

  def ValidatedAction(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action {
      implicit request =>
        val result = for {
          options <- renderOptions(request)
          orgId <- orgIdFromCookie(request)
          if (query(options))
          tokenized <- buildTokenizedRequest(new ObjectId(orgId))
        } yield block(tokenized)
        result.getOrElse(BadRequest("Invalid options or org id"))
    }
}
