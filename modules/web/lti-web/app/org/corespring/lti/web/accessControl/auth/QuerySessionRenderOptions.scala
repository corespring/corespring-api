package org.corespring.lti.web.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.{TokenizedRequest, TokenizedRequestBuilder}
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import org.corespring.player.accessControl.models.RenderOptions
import play.api.mvc.SimpleResult
import scala.concurrent.{ExecutionContext, Future}

/** An implementation of ActionBuilder where we check the user session rendering options */
object QuerySessionRenderOptions extends TokenizedRequestActionBuilder[RenderOptions => Boolean] with PlayerCookieReader with TokenizedRequestBuilder {

  type RenderOptionQuery = (RenderOptions => Boolean)

  import ExecutionContext.Implicits.global
  import play.api.mvc.Results.BadRequest
  import play.api.mvc.{ BodyParser, Action, AnyContent }

  def ValidatedAction(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(query)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(query: RenderOptionQuery)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] =
    Action.async {
      implicit request =>
        val result = for {
          options <- renderOptions(request)
          orgId <- orgIdFromCookie(request)
          if (query(options))
          tokenized <- buildTokenizedRequest(new ObjectId(orgId))
        } yield block(tokenized)
        result.getOrElse(Future(BadRequest("Invalid options or org id")))
    }
}
