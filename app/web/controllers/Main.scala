package web.controllers

import java.util.Date

import org.bson.types.ObjectId
import org.corespring.common.url.BaseUrl
import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ ItemType, WidgetType }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.auth.ApiClientService
import org.corespring.services.item.FieldValueService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.web.common.controllers.deployment.AssetsLoader
import org.corespring.web.common.views.helpers.BuildInfo
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc._
import web.models.WebExecutionContext

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success }

class Main(
  v2Actions: V2Actions,
  fieldValueService: FieldValueService,
  jsonFormatting: JsonFormatting,
  userService: UserService,
  orgService: OrganizationService,
  itemType: ItemType,
  widgetType: WidgetType,
  containerVersionInfo: VersionInfo,
  webExecutionContext: WebExecutionContext,
  playerTokenService: PlayerTokenService,
  buildInfo: BuildInfo,
  assetsLoader: AssetsLoader,
  apiClientService: ApiClientService) extends Controller {

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
      val json = buildInfo.json.deepMerge(Json.obj("container" -> containerVersionInfo.json))
      Ok(json)
    }
  }

  import v2Actions._

  def sampleLaunchCode(id: String) = OrgAction.async {
    request =>
      Future {

        val token = for {
          user <- request.orgAndOpts.user.toSuccess("could not find user")
          apiClient <- apiClientService.getOrCreateForOrg(request.org.id)
          token <- playerTokenService.createToken(apiClient, Json.obj(
            "expires" -> (new Date().getTime + 60 * 60 * 1000),
            "itemId" -> id))
        } yield token

        token match {
          case Success(ctr) =>
            val html = web.views.html.sampleLaunchCode(id, ctr.token, ctr.apiClient, BaseUrl(request))
            Ok(html)
          case Failure(f) =>
            BadRequest("Couldn't generate player token" + f)
        }
      }
  }

  def sessions(orgId: String, month: Option[String]) = RootOrgAction { request =>
    val m: DateTime = month.map(dateString => {
      val Array(year, month) = dateString.split("-")
      new DateTime().withYear(year.toInt).withMonthOfYear(month.toInt)
    }).getOrElse(new DateTime())
    val monthString = DateTimeFormat.forPattern("MMMM, yyyy").print(m)
    val apiKey = DateTimeFormat.forPattern("MM-yyyy").print(m)
    val organization = orgService.findOneById(new ObjectId(orgId)).map(_.name).getOrElse("--")
    Ok(web.views.html.sessions(monthString = monthString, apiKey = apiKey, organization = organization, orgId = orgId))
  }

  def index = OrgAction {
    implicit request =>

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      implicit val writesOrg = jsonFormatting.writeOrg
      implicit val writesRef = jsonFormatting.writeContentCollRef

      defaultValues.map { fv =>
        //Add old 'collections' field
        val org = request.org
        val user = request.orgAndOpts.user
        val legacyJson = Json.obj("collections" -> toJson(org.contentcolls)) ++ toJson(org).as[JsObject]
        val userOrgString = stringify(legacyJson)
        val html = web.views.html.index(dbServer, dbName, user.get, userOrgString, fv, assetsLoader)
        Ok(html)
      }.getOrElse {
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

