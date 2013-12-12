package org.corespring.player.v1.controllers

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.auth.{CheckSessionAccess, TokenizedRequestActionBuilder}
import org.corespring.player.accessControl.models.RequestedAccess
import org.corespring.web.common.controllers.SimpleJsRoutes
import play.api.mvc._

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends Controller with SimpleJsRoutes {

  import org.corespring.api.v1.ItemApi

  def getDetail(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(
    RequestedAccess.asRead(Some(itemId))) { r: TokenizedRequest[AnyContent] => ItemApi.getDetail(itemId)(r) }

  def jsRoutes = Action {
    implicit request =>

     import org.corespring.player.v1.controllers.routes.javascript.{ Item => JsItem }

      val jsRoutes = List(
        JsItem.getDetail)
      Ok(createSimpleRoutes("PlayerItemRoutes", jsRoutes: _*))
        .as("text/javascript")
  }
}

object Item extends Item(CheckSessionAccess)
