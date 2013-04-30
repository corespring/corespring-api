package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.TokenizedRequestActionBuilder
import org.bson.types.ObjectId
import play.api.mvc._
import player.accessControl.auth.{AccessGranterChecker, CheckPlayerSession}
import player.accessControl.models.RequestedAccess


class Item(auth: TokenizedRequestActionBuilder[RequestedAccess] ) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemApi => Api}

  def getDetail(itemId: ObjectId) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))
  )(Api.getDetail(itemId))

  def jsRoutes = Action {
    implicit request =>
      import player.controllers.routes.javascript.{Item => JsItem}
      val jsRoutes = List(
        JsItem.getDetail
      )
      Ok(createSimpleRoutes("PlayerItemRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

object Item extends Item(AccessGranterChecker)
//object Item extends Item(CheckPlayerSession)
