package org.corespring.web.publicsite.controllers

import org.apache.commons.io.FileUtils
import play.api.Play
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(org.corespring.web.publicsite.views.html.index())
  }

  def contact = Action {
    Ok(org.corespring.web.publicsite.views.html.contact())
  }

  def empty = Action {
    request => NotFound("This call has been deprecated - use the new player auth mechanism")
  }

  def partnerships = Action {
    Ok(org.corespring.web.publicsite.views.html.partnerships())
  }

  def about = Action {
    Ok(org.corespring.web.publicsite.views.html.about())
  }

  def getItems = Action {
    val content = FileUtils.readFileToString(Play.getFile("public/public/conf/items.json"), "UTF-8")
    val json = Json.parse(content)
    Ok(json)
  }

  def features = Action {
    Ok(org.corespring.web.publicsite.views.html.features())
  }
}
