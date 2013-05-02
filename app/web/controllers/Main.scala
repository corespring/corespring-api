package web.controllers

import com.mongodb.BasicDBObject
import common.controllers.session.SessionHandler
import controllers.auth.BaseApi
import models.item.Item
import play.api.mvc._
import player.accessControl.cookies.{PlayerCookieKeys, PlayerCookieWriter}
import scala.Some
import web.controllers.utils.ConfigLoader
import web.models.QtiTemplate


object Main extends BaseApi with PlayerCookieWriter with SessionHandler {

  def logout(s: Session) : Session = {
    s - PlayerCookieKeys.RENDER_OPTIONS
  }

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
        .withSession(
          sumSession(request.session, playerCookies(userId.id, userId.providerId) :+ activeModeCookie(): _*)
      )
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

