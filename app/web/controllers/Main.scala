package web.controllers

import org.corespring.legacy.ServiceLookup
import org.corespring.models.{ User }
import play.api.Logger
import play.api.libs.json.{ Json, JsObject }
import play.api.mvc._
import play.api.libs.json.Json._
import securesocial.core.{ SecuredRequest }

object Main extends Controller with securesocial.core.SecureSocial {

  val logger = Logger(Main.getClass)

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  lazy val defaultValues: Option[String] = ServiceLookup.fieldValueService.get.map { fv =>
    implicit val writeFieldValue = ServiceLookup.jsonFormatting.writesFieldValue
    val fvJson = toJson(fv).as[JsObject]
    val values = fvJson.deepMerge(obj(
      "v2ItemTypes" -> bootstrap.Main.itemType.all,
      "widgetTypes" -> bootstrap.Main.widgetType.all))
    stringify(values)
  }

  def index = SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      val userId = request.user.identityId
      val user: User = ServiceLookup.userService.getUser(userId.userId, userId.providerId).getOrElse(throw new RuntimeException("Unknown user"))
      implicit val writesOrg = ServiceLookup.jsonFormatting.writeOrg
      implicit val writesRef = ServiceLookup.jsonFormatting.writeContentCollRef

      logger.info(s"user org: ${user.org}")
      logger.info(s" org: ${ServiceLookup.orgService.findOneById(user.org.orgId)}")
      logger.info(s"default values: $defaultValues")

      (for {
        fv <- defaultValues
        org <- ServiceLookup.orgService.findOneById(user.org.orgId)
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

