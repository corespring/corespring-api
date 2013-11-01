package dev.tools.controllers

import player.controllers.Views
import org.corespring.player.accessControl.auth.{TokenizedRequestActionBuilder, CheckSessionAccess}
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.models.{RenderOptions, RequestedAccess}
import play.api.mvc.{Action, Result, AnyContent, BodyParser}
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import play.api.{Mode, Play}

import play.api.mvc.Results._
import org.corespring.platform.core.models.auth.AccessToken
import org.bson.types.ObjectId
import org.corespring.common.config.AppConfig
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import player.views.models.PlayerParams
import play.api.templates.Html

object DevActionBuilder extends TokenizedRequestActionBuilder[RequestedAccess] with PlayerCookieWriter{

  def ValidatedAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] = Action{ implicit request =>

    def devToolsEnabled = Play.current.mode == Mode.Dev || Play.current.configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

    def loadDevPlayer = {

      val orgId = request.getQueryString("orgId").getOrElse("502404dd0364dc35bb39339a")

      if(ObjectId.isValid(orgId)){
        val id = new ObjectId(orgId)

        AccessToken.getTokenForOrgById(id).map{ t =>
          val tokenizedRequest = TokenizedRequest(t.tokenId, request)
          val orgId = AppConfig.demoOrgId
          val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.All)
          val newSession = sumSession(request.session, newCookies: _*)

          val result = block(tokenizedRequest)
          result.withSession(newSession)
        }.getOrElse(NotFound(""))
      } else NotFound("")
    }

    if(devToolsEnabled) loadDevPlayer else NotFound("")
  }
}


object DevViews extends Views(DevActionBuilder, ItemServiceImpl, QuizService){


  override def defaultTemplate: (PlayerParams => Html) = (p) => dev.tools.views.html.DevPlayer(p)
}


