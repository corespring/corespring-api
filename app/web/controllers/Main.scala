package web.controllers

import play.api.mvc._
import web.models.QtiTemplate
import web.controllers.utils.ConfigLoader
import web.controllers.services.DBConnect
import play.api.data.Form
import play.api.data.Forms._
import scala.Some
import scala.Tuple2

import play.api.libs.json.Json._
import models.{Item, FieldValue, User}
import models.auth.AccessToken
import play.api.libs.json.Json
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.{BasicDBObject, DBObject}

object Main extends Controller with Secured {

  val TMP_ACCESS_TOKEN : String = "34dj45a769j4e1c0h4wb"

  def previewItem(itemId:String) = Action{ request =>
    Ok(web.views.html.itemPreview(itemId, TMP_ACCESS_TOKEN))
  }


  /**
   * A temporary route whilst working on preview
   * @return
   */
  def previewAnyItem() = {
    Item.findOne( new BasicDBObject()) match {
      case Some(item) => previewItem(item.id.toString)
      case None => Action(Ok("no item found"))
    }
  }

  def renderProfile(itemId:String) = Action{ request =>
    Ok(web.views.html.profilePrint(itemId, TMP_ACCESS_TOKEN))
  }


  def index = IsAuthenticated {
    username => _ =>
      val jsonString = getFieldValueJsonString
      val (dbServer, dbName) = getDbName(ConfigLoader.get("mongodb.default.uri"))
      Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, username, jsonString, TMP_ACCESS_TOKEN))
  }

  private def getFieldValueJsonString: String = {
    val all = FieldValue.findAll().toList
    val first = all(0)
    com.codahale.jerkson.Json.generate(first)
  }

  def getAccessToken = IsAuthenticated {
    username => request =>
      User.getUser(username) match {
        case Some(user) => {
          //TODO: Just hardcoding this for now until we agree how to connect the user to the token.
          Ok(toJson(Map("access_token" -> "34dj45a769j4e1c0h4wb")))
        }
        case None => Forbidden
      }

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

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying("Invalid email or password", result => result match {
      case (username, password) => User.getUser(username).isDefined
    })
  )

  def login = Action {
    implicit request =>
      Ok(web.views.html.login(loginForm))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => BadRequest(web.views.html.login(formWithErrors)),
        user => Redirect(web.controllers.routes.Main.index()).withSession("username" -> user._1)
      )
  }

  def logout = Action {
    Redirect(web.controllers.routes.Main.login()).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def userInfo = IsAuthenticated {
    username => _ =>
      User.getUser(username).map {
        user =>
          Ok("user is: " + user)
      }.getOrElse(Forbidden)
  }
}


/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("username")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(web.controllers.routes.Main.login())

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    user =>
      Action(request => f(user)(request))
  }

  //TODO: Flesh this out.
  def isLoggedIn(request: RequestHeader): Boolean = request.session.get("username") != null

}

