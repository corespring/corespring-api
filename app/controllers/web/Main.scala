package controllers.web

import models.web.User

import play.api.mvc._
import models.web.QtiTemplate
import controllers.web.services.DBConnect
import utils.ConfigLoader
import play.api.data.Form
import play.api.data.Forms._
import views.html
import scala.Some
import scala.Tuple2

object Main extends Controller with Secured {

  def index =  IsAuthenticated{ username => _ =>
      val obj = DBConnect.getCollection("mongodb://localhost:27017/api", "fieldValues").findOne()
      val jsonString = com.codahale.jerkson.Json.generate(obj)
      val (dbServer, dbName) = getDbName(ConfigLoader.get("MONGO_URI"))
      Ok(views.html.web.index(QtiTemplate.all(), dbServer, dbName, "ed", jsonString))
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
      case (username, password) => User.authenticate(username, password).isDefined
    })
  )

  def login = Action {
    implicit request =>
      Ok(views.html.web.login(loginForm))
  }

  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.web.login(formWithErrors)),
        user => Redirect(routes.Main.index()).withSession("username" -> user._1)
      )
  }

  def logout = Action {
    Redirect(routes.Main.login()).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  def userInfo = IsAuthenticated {
    username => _ =>
      User.findByEmail(username).map {
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
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Main.login())

  // --

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    user =>
      Action(request => f(user)(request))
  }

  /**
   * return a user or null
   */
  def getUser(request: RequestHeader): User = {

    if (!isLoggedIn(request)) {
      return null
    }

    username(request) match {
      case None => null
      case Some(x) => {
        User.findByEmail(x) match {
          case None => null
          case Some(x) => x
        }
      }
    }
  }

  def isLoggedIn(request: RequestHeader): Boolean = request.session.get("email") != null

}

