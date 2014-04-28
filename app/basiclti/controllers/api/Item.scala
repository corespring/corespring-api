package basiclti.controllers.api

import basiclti.accessControl.auth.QuerySessionRenderOptions
import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc.{ AnyContent, Controller }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.TokenizedRequestActionBuilder
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.platform.core.models.versioning.VersionedIdImplicits.Binders.versionedIdToString

class Item(auth: TokenizedRequestActionBuilder[QuerySessionRenderOptions.RenderOptionQuery]) extends Controller with SimpleJsRoutes {

  import api.v1.{ ItemApi => Api }

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction(o => o.allowItemId("*")) { r: TokenizedRequest[AnyContent] =>
      Api.list(q, f, c, sk, l, sort)(r)
    }

  def read(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(o => o.allowItemId(itemId.toString())) { r: TokenizedRequest[AnyContent] =>
    Api.getDetail(itemId)(r)
  }
}

object Item extends Item(QuerySessionRenderOptions)
