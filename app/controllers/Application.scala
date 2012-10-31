package controllers

import play.api.mvc._
import play.api.libs.json.Json
import play.api.Play
import io.Codec
import java.nio.charset.Charset
import play.api.Play.current

object Application extends Controller {

  def index = Action {
    Ok(views.html.index())
  }
  def contact = Action {
    Ok(views.html.contact())
  }
  def collection = Action {
    Ok(views.html.collection())
  }
  def partnerships = Action {
    Ok(views.html.partnerships())
  }
  def team = Action {
    Ok(views.html.team())
  }
  def getItems = Action {
    Ok(Json.parse(io.Source.fromFile(Play.getFile("conf/seed-data/items.json"))(new Codec(Charset.forName("UTF-8"))).mkString))
  }

}
