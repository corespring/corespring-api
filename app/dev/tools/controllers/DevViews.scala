package dev.tools.controllers

import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import org.corespring.player.accessControl.models.{RenderOptions, RequestedAccess}
import play.api.mvc.Results._
import play.api.mvc._
import play.api.templates.Html
import play.api.{Mode, Play}
import player.controllers.Views
import player.views.models.PlayerParams
import scala.Some
import scala.concurrent.{ExecutionContext, Future}


object DevActionBuilder extends TokenizedRequestActionBuilder[RequestedAccess] with PlayerCookieWriter{

  def ValidatedAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async{ implicit request =>

    import ExecutionContext.Implicits.global

    def devToolsEnabled = Play.current.mode == Mode.Dev || Play.current.configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

    def loadDevPlayer : Future[SimpleResult] = {

      val orgId = request.getQueryString("orgId").getOrElse("502404dd0364dc35bb39339a")
      
      def makeRequest(tokenId:String) = {
        val tokenizedRequest = TokenizedRequest(tokenId, request)
        Some(tokenizedRequest)
      }

      def makeSession(r:Request[AnyContent]) = {
        val orgId = AppConfig.demoOrgId
        val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.All)
        Some(sumSession(request.session, newCookies: _*))
      }
      
      val out = for {
        oid <- if(ObjectId.isValid(orgId)) Some(orgId) else None
        token <- AccessToken.getTokenForOrgById(new ObjectId(oid))
        request <- makeRequest(token.tokenId)
        session <- makeSession(request)
      } yield block(request).map(_.withSession(session))

      out.getOrElse(Future(NotFound))
    }

    if(devToolsEnabled) loadDevPlayer else Future(NotFound(""))
  }
}


object DevViews extends Views(DevActionBuilder, ItemServiceImpl, QuizService){


  override def defaultTemplate: (PlayerParams => Html) = (p) => dev.tools.views.html.DevPlayer(p)
}


