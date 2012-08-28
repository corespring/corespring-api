package controllers.web

import play.api.mvc.{Action, Controller}

object Main extends Controller{

  def index = Action{ request =>
    Ok("hello")
  }

}
