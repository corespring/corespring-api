package assetTest.controllers

import play.api.mvc.{Action, Controller}

object Main extends Controller{

  def index = Action{ request => Ok(assetTest.views.html.page())}

}
