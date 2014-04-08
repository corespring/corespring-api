package player.controllers

import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.{ CheckSessionAccess, TokenizedRequestActionBuilder }
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.models.RequestedAccess

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends Controller with SimpleJsRoutes {

  import api.v1.{ ItemApi => Api }

  def getDetail(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))) { r: TokenizedRequest[AnyContent] => Api.getDetail(itemId)(r) }

  def jsRoutes = Action {
    implicit request =>
      import player.controllers.routes.javascript.{ Item => JsItem }
      val jsRoutes = List(
        JsItem.getDetail)
      Ok(createSimpleRoutes("PlayerItemRoutes", request, jsRoutes: _*))
        .as("text/javascript")
  }
}

object Item extends Item(CheckSessionAccess)
