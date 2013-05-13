package internal.controllers

import play.api.mvc.{Action, Controller}


object BlitzAuth extends Controller{
  def index = Action{ request =>
    Ok("42")
  }
}
