package web.controllers

import org.corespring.itemSearch.AggregateType.{ItemType, WidgetType}
import org.corespring.models.User
import org.corespring.models.json.JsonFormatting
import org.corespring.services.item.FieldValueService
import org.corespring.services.{OrganizationService, UserService}
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc._
import securesocial.core.SecuredRequest

class TestSupport(
  userService: UserService,
  orgService: OrganizationService) extends Controller with securesocial.core.SecureSocial {

  val logger = Logger(classOf[TestSupport])

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  def getAllRoutes = SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>

      val userId = request.user.identityId
      val user: User = userService.getUser(userId.userId, userId.providerId).getOrElse(throw new RuntimeException("Unknown user"))

      implicit val implicitWrites = new Writes[(String, String, String)] {
        def writes(a:(String, String, String)): JsValue = {
          Json.obj(
            "method" -> a._1,
            "route" -> a._2,
            "impl" -> a._3)
        }
      }

      (for {
        org <- orgService.findOneById(user.org.orgId)
        app <- play.api.Play.maybeApplication
        routes <- app.routes
        docs <- Some(routes.documentation)
        json <- Some(Json.obj("routes" -> Json.toJson(docs)))
      } yield json) match {
        case Some(js) => Ok(js)
        case _ => InternalServerError("could not retrieve list of routes")
      }
  }
}

