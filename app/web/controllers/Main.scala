package web.controllers

import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ WidgetType, ItemType }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ User }
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.services.item.FieldValueService
import org.corespring.web.common.views.helpers.BuildInfo
import play.api.Logger
import play.api.libs.json.{ JsValue, Json, JsObject }
import play.api.mvc._
import play.api.libs.json.Json._
import securesocial.core.{ SecuredRequest }
import web.models.{ WebExecutionContext, ContainerVersion }

import scala.concurrent.Future

class Main(
  fieldValueService: FieldValueService,
  jsonFormatting: JsonFormatting,
  userService: UserService,
  orgService: OrganizationService,
  itemType: ItemType,
  widgetType: WidgetType,
  containerVersionInfo: ContainerVersion,
  webExecutionContext: WebExecutionContext) extends Controller with securesocial.core.SecureSocial {

  implicit val context = webExecutionContext.context

  val logger = Logger(classOf[Main])

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  private lazy val fieldValues: Option[JsObject] = fieldValueService.get.map {
    fv =>
      implicit val writeFieldValue = jsonFormatting.writesFieldValue
      toJson(fv).as[JsObject]
  }

  def defaultValues: Option[String] = fieldValues.map { fvJson =>
    val values = fvJson.deepMerge(obj(
      "v2ItemTypes" -> itemType.all,
      "widgetTypes" -> widgetType.all))
    stringify(values)
  }

  def version = Action.async {
    Future {
      val json = BuildInfo.json.deepMerge(Json.obj("container" -> containerVersionInfo.json))
      Ok(json)
    }
  }

  def index = SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      val userId = request.user.identityId
      val user: User = userService.getUser(userId.userId, userId.providerId).getOrElse(throw new RuntimeException("Unknown user"))
      implicit val writesOrg = jsonFormatting.writeOrg
      implicit val writesRef = jsonFormatting.writeContentCollRef

      (for {
        fv <- defaultValues
        org <- orgService.findOneById(user.org.orgId)
      } yield {
        //Add old 'collections' field
        val legacyJson = Json.obj("collections" -> toJson(org.contentcolls)) ++ toJson(org).as[JsObject]
        val userOrgString = stringify(legacyJson)
        val html = web.views.html.index(dbServer, dbName, user, userOrgString, fv)
        Ok(html)
      }).getOrElse {
        InternalServerError("could not find organization of user")
      }
  }

  private def getDbName(uri: Option[String]): (String, String) = uri match {
    case Some(url) => {
      if (!url.contains("@")) {
        val noAt = """mongodb://(.*)/(.*)""".r
        val noAt(server, name) = url
        (server, name)
      } else {
        val withAt = """.*@(.*)/(.*)""".r
        val withAt(server, name) = url
        (server, name)
      }
    }
    case None => ("?", "?")
  }
}

