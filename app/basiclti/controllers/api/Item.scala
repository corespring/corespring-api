package basiclti.controllers.api

import basiclti.controllers.auth.QuerySessionRenderOptions
import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc.Controller
import player.controllers.auth.AuthenticateAndUseToken


class Item(auth: AuthenticateAndUseToken[QuerySessionRenderOptions.RenderOptionQuery]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemApi => Api}

  def list(q: Option[String], f: Option[String], c: String, sk: Int, l: Int, sort: Option[String]) =
    auth.OrgAction( o => o.allowItemId("*"))(Api.list(q, f, c, sk, l, sort))

  def read(itemId: ObjectId) = auth.OrgAction(o => o.allowItemId(itemId.toString))(Api.getDetail(itemId))
}


object Item extends Item(QuerySessionRenderOptions)
