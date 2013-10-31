package dev.tools.controllers

import player.controllers.Views
import org.corespring.player.accessControl.auth.{TokenizedRequestActionBuilder, CheckSessionAccess}
import org.corespring.platform.core.services.item.ItemServiceImpl
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.models.RequestedAccess
import play.api.mvc.{Action, Result, AnyContent, BodyParser}
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import play.api.{Mode, Play}

import play.api.mvc.Results._
import org.corespring.platform.core.models.auth.AccessToken
import org.bson.types.ObjectId

object DevActionBuilder extends TokenizedRequestActionBuilder[RequestedAccess]{

  def ValidatedAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(access: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] = Action{ request =>

    if(Play.current.mode != Mode.Dev){
      NotFound("")
    } else {

      val orgId = request.getQueryString("orgId").getOrElse("502404dd0364dc35bb39339a")

      if(ObjectId.isValid(orgId)){
        val id = new ObjectId(orgId)

        AccessToken.getTokenForOrgById(id).map{ t =>
          val tokenizedRequest = TokenizedRequest(t.tokenId, request)
          val result = block(tokenizedRequest)
          result
        }.getOrElse(NotFound(""))

      } else {
        NotFound("")
      }
    }
  }
}


object DevViews extends Views(DevActionBuilder, ItemServiceImpl, QuizService)


