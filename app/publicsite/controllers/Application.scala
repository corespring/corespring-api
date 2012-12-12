package publicsite.controllers

import play.api.mvc._
import play.api.Play
import play.api.libs.json.Json
import play.api.Play.current
import java.nio.charset.Charset
import scala.io.Codec
import controllers.Log
import controllers.auth.OAuthConstants


object Application extends Controller with securesocial.core.SecureSocial {

  def index = Action {
    Ok(publicsite.views.html.index())
  }
  def contact = Action {
    Ok(publicsite.views.html.contact())
  }
  def educators = UserAwareAction { implicit request =>
    request.user match {
      case Some(user) => Ok(publicsite.views.html.educators())
      case _ => Ok(publicsite.views.html.educators()).withSession(OAuthConstants.AccessToken -> common.mock.MockToken)
    }
  }
  def partnerships = Action {
    Ok(publicsite.views.html.partnerships())
  }
  def about = Action {
    Ok(publicsite.views.html.about())
  }
  def getItems = Action {
    Ok(Json.parse(io.Source.fromFile(Play.getFile("public/public/conf/items.json"))(new Codec(Charset.forName("UTF-8"))).mkString))
  }
}
