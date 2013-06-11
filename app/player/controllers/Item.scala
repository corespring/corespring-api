package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.TokenizedRequestActionBuilder
import org.bson.types.ObjectId
import play.api.mvc._
import player.accessControl.auth.{CheckSessionAccess, CheckSession}
import player.accessControl.models.RequestedAccess


class Item(auth: TokenizedRequestActionBuilder[RequestedAccess] ) extends Controller with SimpleJsRoutes {

  import api.v1.{NewItemApi => NewApi}
  def getDetail(itemId: ObjectId, version : Option[Int] = None) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))
  )(NewApi.getDetail(itemId, version))

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

object Item extends Item(CheckSessionAccess)
