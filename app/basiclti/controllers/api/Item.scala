package basiclti.controllers.api

import basiclti.accessControl.auth.QuerySessionRenderOptions
import common.controllers.SimpleJsRoutes
import controllers.auth.TokenizedRequestActionBuilder
import org.bson.types.ObjectId
import play.api.mvc.Controller
import org.corespring.platform.data.mongo.models.VersionedId


class Item(auth: TokenizedRequestActionBuilder[QuerySessionRenderOptions.RenderOptionQuery]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemApi => Api}

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.ValidatedAction( o => o.allowItemId("*"))(Api.list(q, f, c, sk, l, sort))

  def read(itemId: VersionedId[ObjectId]) = auth.ValidatedAction(o => o.allowItemId(itemId.toString))(Api.getDetail(itemId))
}


object Item extends Item(QuerySessionRenderOptions)
