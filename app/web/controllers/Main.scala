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
import models.User
import models.auth.AccessToken
import play.api.libs.json.Json

object Main extends Controller with Secured {

  def index =  IsAuthenticated{ username => _ =>
      val obj = DBConnect.getCollection("mongodb://localhost:27017/api", "fieldValues").findOne()
      val jsonString = com.codahale.jerkson.Json.generate(obj)
      val (dbServer, dbName) = getDbName(ConfigLoader.get("MONGO_URI"))
      Ok( web.views.html.index(QtiTemplate.all(), dbServer, dbName, username, jsonString, "34dj45a769j4e1c0h4wb"))
  }


  def getAccessToken = IsAuthenticated{
    username => request =>
    User.getUser(username) match {
      case Some(user) => {
        //TODO: Just hardcoding this for now until we agree how to connect the user to the token.
        Ok(toJson( Map("access_token" -> "34dj45a769j4e1c0h4wb")) )
      }
      case None => Forbidden
    }

  }

  private def getDbName(uri: Option[String]): Tuple2[String, String] = uri match {
    case Some(url) => {
      val NameRegex = """.*@(.*)/(.*)""".r
      val NameRegex(server, name) = url
      (server, name)
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
        formWithErrors => BadRequest( web.views.html.login(formWithErrors)),
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

