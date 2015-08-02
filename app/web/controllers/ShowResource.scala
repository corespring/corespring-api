package web.controllers

import org.corespring.legacy.ServiceLookup
import org.corespring.assets.{ CorespringS3ServiceExtended, CorespringS3Service }
import org.corespring.common.mongo.ObjectIdParser
import org.corespring.services.item.{ ItemService, ItemServiceClient }
import play.api.mvc.{ AnyContent, Action, Controller }

object ShowResource
  extends Controller
  with ObjectIdParser
  with ItemServiceClient {

  def s3Service: CorespringS3Service = CorespringS3ServiceExtended

  def itemService: ItemService = ServiceLookup.itemService

  def renderDataResourceForPrinting(itemId: String) = Action(BadRequest)
  def getDefaultResourceFile(itemId: String, resourceName: String) = Action(BadRequest)
  def getResourceFile(itemId: String, resourceName: String, filename: String) = Action(BadRequest)

  def javascriptRoutes = Action {
    implicit request =>

      import play.api.Routes
      import web.controllers.routes.javascript.{ ShowResource => ShowResourceJs }
      import web.controllers.routes.javascript.{ Partials => PartialsJs }
      Ok(
        Routes.javascriptRouter("WebRoutes")(
          PartialsJs.editItem,
          ShowResourceJs.getResourceFile)).as("text/javascript")
  }

}
