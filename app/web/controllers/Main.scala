package web.controllers

import java.util.Date

import com.softwaremill.macwire.MacwireMacros._
import org.bson.types.ObjectId
import org.corespring.common.url.BaseUrl
import org.corespring.container.client.VersionInfo
import org.corespring.itemSearch.AggregateType.{ WidgetType, ItemType }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ User }
import org.corespring.services.auth.ApiClientService
import org.corespring.services.{ OrganizationService, UserService }
import org.corespring.services.item.FieldValueService
import org.corespring.v2.api.services.PlayerTokenService
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.web.common.controllers.deployment.AssetsLoader
import org.corespring.web.common.views.helpers.BuildInfo
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.json.{ JsString, JsValue, Json, JsObject }
import play.api.mvc._
import play.api.libs.json.Json._
import securesocial.core.SecuredRequest
import web.models.{ WebExecutionContext, ContainerVersion }
import scalaz.Scalaz._

import scala.concurrent.Future
import scalaz.Success
import scalaz.Failure

class Main(
  fieldValueService: FieldValueService,
  jsonFormatting: JsonFormatting,
  userService: UserService,
  orgService: OrganizationService,
  itemType: ItemType,
  widgetType: WidgetType,
  containerVersionInfo: ContainerVersion,
  webExecutionContext: WebExecutionContext,
  playerTokenService: PlayerTokenService,
  userSessionOrgIdentity: UserSessionOrgIdentity,
  buildInfo: BuildInfo,
  assetsLoader: AssetsLoader,
  apiClientService: ApiClientService) extends Controller with securesocial.core.SecureSocial {

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

  def sampleLaunchCode(id: String) = Action.async {
    request =>
      Future {

        val token = for {
          maybeUser <- userSessionOrgIdentity(request).map(_.user)
          user <- maybeUser.toSuccess("could not find user")
          apiClient <- apiClientService.getOrCreateForOrg(user.org.orgId)
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

  def sessions(orgId: String, month: Option[String]) = SecuredAction { request =>
    userSessionOrgIdentity(request) match {
      case Success(orgAndOpts) if (orgAndOpts.org.id == jsonFormatting.rootOrgId.toString) => {
        val m: DateTime = month.map(dateString => {
          val Array(year, month) = dateString.split("-")
          new DateTime().withYear(year.toInt).withMonthOfYear(month.toInt)
        }).getOrElse(new DateTime())
        val monthString = DateTimeFormat.forPattern("MMMM, yyyy").print(m)
        val apiKey = DateTimeFormat.forPattern("MM-yyyy").print(m)
        val organization = orgService.findOneById(new ObjectId(orgId)).map(_.name).getOrElse("--")
        Ok(web.views.html.sessions(monthString = monthString, apiKey = apiKey, organization = organization, orgId = orgId))
      }
      case _ => Unauthorized("Please contact a CoreSpring representative for access to monthly session data.")
    }
  }

  private def AdminAction(block: SecuredRequest[AnyContent] => SimpleResult) = SecuredAction {
    implicit request: SecuredRequest[AnyContent] => {
      userSessionOrgIdentity(request) match {
        case Success(orgAndOpts) if (orgAndOpts.org.id == jsonFormatting.rootOrgId.toString) => block(request)
        case _ => Unauthorized("Please contact a CoreSpring representative for access.")
      }
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
        val html = web.views.html.index(dbServer, dbName, user, userOrgString, fv, assetsLoader)
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

