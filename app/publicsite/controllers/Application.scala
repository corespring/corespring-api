package publicsite.controllers

import play.api.mvc._
import play.api.Play
import play.api.libs.json.Json
import play.api.Play.current
import java.nio.charset.Charset
import scala.io.Codec
import controllers.Log
import controllers.auth.OAuthConstants


object Application extends Controller{

  //TODO: Temporary mock access token
  val MOCK_ACCESS_TOKEN_ID: String = "34dj45a769j4e1c0h4wb"


  def index = Action {
    Ok(publicsite.views.html.index())
  }
  def contact = Action {
    Ok(publicsite.views.html.contact())
  }
  def collection = Action {
    Ok(publicsite.views.html.collection())
    .withSession(OAuthConstants.AccessToken -> MOCK_ACCESS_TOKEN_ID)
  }
  def partnerships = Action {
    Ok(publicsite.views.html.partnerships())
  }
  def team = Action {
    Ok(publicsite.views.html.team())
  }
  def getItems = Action {
    Ok(Json.parse(io.Source.fromFile(Play.getFile("public/public/conf/items.json"))(new Codec(Charset.forName("UTF-8"))).mkString))
  }
}
