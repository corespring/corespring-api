package web.controllers

import java.util.Date

import org.bson.types.ObjectId
import org.corespring.csApi.buildInfo.BuildInfo
import org.corespring.common.url.BaseUrl
import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ WidgetType, ItemType }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ User }
import org.corespring.services.auth.ApiClientService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.itemSearch.AggregateType.{ ItemType, WidgetType }
import org.corespring.models.User
import org.corespring.models.json.JsonFormatting
import org.corespring.services.item.FieldValueService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.web.common.controllers.deployment.AssetsLoader
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.mvc._
import play.api.libs.json.Json._
import securesocial.core.SecuredRequest
import web.models.{ WebExecutionContext }
import scalaz.Scalaz._
import play.api.libs.json.Json._
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc._
import web.models.WebExecutionContext

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success }

class Main(
  actions: V2Actions,
  fieldValueService: FieldValueService,
  jsonFormatting: JsonFormatting,
  userService: UserService,
  orgService: OrganizationService,
  itemType: ItemType,
  widgetType: WidgetType,
  containerVersionInfo: VersionInfo,
  webExecutionContext: WebExecutionContext,
  playerTokenService: PlayerTokenService,
  assetsLoader: AssetsLoader) extends Controller {

  implicit val context = webExecutionContext.context

  val logger = Logger(classOf[Main])

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  private lazy val fieldValues: Option[JsObject] = fieldValueService.get.map {
    fv =>
      implicit val writeFieldValue = jsonFormatting.writesFieldValue
      toJson(fv).as[JsObject]
  }

  def defaultValues(collections: Seq[String] = Seq.empty): Option[String] = fieldValues.map { fvJson =>
    val values = fvJson.deepMerge(obj(
      "v2ItemTypes" -> itemType.all(collections),
      "widgetTypes" -> widgetType.all(collections)))
    stringify(values)
  }

  def version = Action.async {
    Future {
      val json = Json.parse(BuildInfo.toJson).as[JsObject].deepMerge(obj("container" -> containerVersionInfo.json))
      Ok(json)
    }
  }

  def iconsAndColorsPage() = actions.SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>
      val html = web.views.html.iconsAndColorsPage()
      Ok(html)
  }

  def sampleLaunchCode(id: String) = actions.OrgAndApiClient.async {
    request =>
      Future {

        val token = for {
          user <- request.orgAndOpts.user.toSuccess("could not find user")
          token <- playerTokenService.createToken(request.apiClient, Json.obj(
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

  def sessions(orgId: String, month: Option[String]) = actions.RootOrg { request =>
    val m: DateTime = month.map(dateString => {
      val Array(year, month) = dateString.split("-")
      new DateTime().withYear(year.toInt).withMonthOfYear(month.toInt)
    }).getOrElse(new DateTime())
    val monthString = DateTimeFormat.forPattern("MMMM, yyyy").print(m)
    val apiKey = DateTimeFormat.forPattern("MM-yyyy").print(m)
    val organization = orgService.findOneById(new ObjectId(orgId)).map(_.name).getOrElse("--")
    Ok(web.views.html.sessions(monthString = monthString, apiKey = apiKey, organization = organization, orgId = orgId))
  }

  def index = actions.SecuredAction {
    implicit request =>

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      implicit val writesOrg = jsonFormatting.writeOrg
      implicit val writesRef = jsonFormatting.writeContentCollRef

      val maybeHtml = for {
        user <- userService.getUser(request.user.identityId.userId, request.user.identityId.providerId)
        org <- orgService.findOneById(user.org.orgId)
        fv <- defaultValues(org.contentcolls.map(_.collectionId.toString))
        legacyJson <- Some(Json.obj("collections" -> toJson(org.contentcolls)) ++ toJson(org).as[JsObject])
        userOrgString <- Some(stringify(legacyJson))
        html <- Some(web.views.html.index(dbServer, dbName, user, userOrgString, fv, assetsLoader))
      } yield html

      maybeHtml.map { html =>
        Ok(html)
      }.getOrElse(InternalServerError("could not find organization of user"))
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

