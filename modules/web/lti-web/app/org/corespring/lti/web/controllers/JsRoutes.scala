package org.corespring.lti.web.controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import org.corespring.web.common.controllers.SimpleJsRoutes

object JsRoutes extends Controller with SimpleJsRoutes {

  def index = Action { request =>

    import api.v1.routes.javascript.{ Collection => JsCollection }
    import api.v1.routes.javascript.{ Item => JsItem }

    Ok(
      createSimpleRoutes("PlayerCollectionRoutes", JsCollection.list) +
      createSimpleRoutes("PlayerItemRoutes", JsItem.list)).as("text/javascript")
  }

}
