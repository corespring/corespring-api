package web.controllers

import play.api.mvc._
import web.models.QtiTemplate
import web.controllers.utils.ConfigLoader
import scala.Some
import scala.Tuple2

import com.mongodb.BasicDBObject
import securesocial.core.SecureSocial
import models.item.Item
import controllers.auth.BaseApi

object Main extends BaseApi {


  def previewItem(itemId:String, defaultView:String = "profile") = ApiAction { request =>
    println("Default view is"+defaultView)
    Ok(web.views.html.itemPreview(itemId, defaultView = defaultView))
  }


  /**
   * A temporary route whilst working on preview
   * @return
   */
  def previewAnyItem() = ApiAction {
    Item.findOne( new BasicDBObject()) match {
      case Some(item) => previewItem(item.id.toString)
      case None => Action(Ok("no item found"))
    }
  }

  def renderProfile(itemId:String) = Action { request =>
    Ok(web.views.html.profilePrint(itemId, common.mock.MockToken))
  }


  def index = SecuredAction { request =>
      val (dbServer, dbName) = getDbName(ConfigLoader.get("mongodb.default.uri"))
      Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, request.user.fullName,  common.mock.MockToken))
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

