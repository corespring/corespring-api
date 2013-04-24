package player.controllers

import common.controllers.SimpleJsRoutes
import controllers.auth.{BaseRender}
import org.bson.types.ObjectId
import play.api.mvc._
import player.controllers.auth.{CheckPlayerSession, RequestedAccess, Authenticate}


class Item(auth: Authenticate[AnyContent]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemApi => Api}

  def read(itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
  )(Api.getDetail(itemId))

  def jsRoutes = Action {
    implicit request =>
      import routes.javascript.{Item => JsItem}
      val jsRoutes = List(
        JsItem.read
      )
      Ok(createSimpleRoutes("PlayerItemRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

object Item extends Item(CheckPlayerSession)
