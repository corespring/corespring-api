package web.controllers

import play.api.mvc._
import web.models.QtiTemplate
import web.controllers.utils.ConfigLoader
import scala.Some
import scala.Tuple2

import models.{Item, FieldValue}
import com.mongodb.BasicDBObject
import securesocial.core.SecureSocial

object Main extends Controller with SecureSocial {

  val TMP_ACCESS_TOKEN : String = "-- delete me ---"

  def previewItem(itemId:String) = Action{ request =>
    Ok(web.views.html.itemPreview(itemId, TMP_ACCESS_TOKEN))
  }


  /**
   * A temporary route whilst working on preview
   * @return
   */
  def previewAnyItem() = SecuredAction() {
    Item.findOne( new BasicDBObject()) match {
      case Some(item) => previewItem(item.id.toString)
      case None => Action(Ok("no item found"))
    }
  }

  def renderProfile(itemId:String) = SecuredAction() { request =>
    Ok(web.views.html.profilePrint(itemId, TMP_ACCESS_TOKEN))
  }


  def index = SecuredAction() { request =>
      val jsonString = getFieldValueJsonString
      val (dbServer, dbName) = getDbName(ConfigLoader.get("mongodb.default.uri"))
      Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, request.user.fullName, jsonString, TMP_ACCESS_TOKEN))
  }

  private def getFieldValueJsonString: String = {
    val all = FieldValue.findAll().toList
    val first = all(0)
    com.codahale.jerkson.Json.generate(first)
  }

  private def getDbName(uri: Option[String]): Tuple2[String, String] = uri match {
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

