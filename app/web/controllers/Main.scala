package web.controllers

import com.mongodb.BasicDBObject
import controllers.auth.{RenderOptions, BaseApi}
import models.item.Item
import models.{UserOrg, User}
import play.api.libs.json.Json
import play.api.mvc._
import scala.Some
import securesocial.core.SecuredRequest
import web.controllers.utils.ConfigLoader
import web.models.QtiTemplate
import player.rendering.PlayerCookieWriter
import play.api.templates.Html


object Main extends BaseApi with PlayerCookieWriter {


  def previewItem(itemId: String, defaultView: String = "profile") = ApiAction {
    request =>
      println("Default view is" + defaultView)
      Ok(web.views.html.itemPreview(itemId, defaultView = defaultView))
  }


  /**
   * A temporary route whilst working on preview
   * @return
   */
  def previewAnyItem() = ApiAction {
    Item.findOne(new BasicDBObject()) match {
      case Some(item) => previewItem(item.id.toString)
      case None => Action(Ok("no item found"))
    }
  }

  def renderProfile(itemId: String) = Action {
    request =>
      Ok(web.views.html.profilePrint(itemId, common.mock.MockToken))
  }

  def index = SecuredAction {
    implicit request =>
      val (dbServer, dbName) = getDbName(ConfigLoader.get("mongodb.default.uri"))
      val userId = request.user.id
      Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, request.user.fullName, "remove"))
        .withSession(playerSession(userId.id, userId.providerId))
  }

  private def getDbName(uri: Option[String]): (String, String) = uri match {
    case Some(url) => {
      if (!url.contains("@")) {
        val noAt = """mongodb://(.*)/(.*)""".r
        val noAt(server, name) = url
        (server, name)
      }
      else {
        val withAt = """.*@(.*)/(.*)""".r
        val withAt(server, name) = url
        (server, name)
      }
    }
    case None => ("?", "?")
  }
}

