package basiclti.controllers

import common.controllers.SimpleJsRoutes
import play.api.mvc.Action
import play.api.mvc.Controller

object JsRoutes extends Controller with SimpleJsRoutes {

  def index = Action { request =>

    import api.routes.javascript.{ Collection => JsCollection }
    import api.routes.javascript.{ Item => JsItem }

    Ok(
      createSimpleRoutes("PlayerCollectionRoutes",request, JsCollection.list) +
        createSimpleRoutes("PlayerItemRoutes", request, JsItem.list)).as("text/javascript")
  }

}
