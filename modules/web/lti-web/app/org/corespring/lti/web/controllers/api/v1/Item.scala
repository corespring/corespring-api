package org.corespring.lti.web.controllers.api.v1

import org.bson.types.ObjectId
import org.corespring.api.v1.{ ItemApi => Api }
import org.corespring.lti.web.accessControl.auth.QuerySessionRenderOptions
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.web.common.controllers.SimpleJsRoutes
import play.api.mvc.{ AnyContent, Controller }

class Item(auth: TokenizedRequestActionBuilder[QuerySessionRenderOptions.RenderOptionQuery]) extends Controller with SimpleJsRoutes {

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction(o => o.allowItemId("*")) { r: TokenizedRequest[AnyContent] =>
      Api.list(q, f, c, sk, l, sort)(r)
    }

  def read(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(o => o.allowItemId(itemId.toString())) { r: TokenizedRequest[AnyContent] =>
    Api.getDetail(itemId)(r)
  }
}

object Item extends Item(QuerySessionRenderOptions)
