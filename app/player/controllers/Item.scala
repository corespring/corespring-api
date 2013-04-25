package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.{BaseRender}
import org.bson.types.ObjectId
import play.api.mvc._
import player.controllers.auth.{PlayerAuthenticate, CheckPlayerSession, RequestedAccess, Authenticate}


class Item(auth: PlayerAuthenticate) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemApi => Api}

  def getDetail(itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
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

object Item extends Item(CheckPlayerSession)
