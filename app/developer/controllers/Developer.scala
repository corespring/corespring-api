package developer.controllers

import play.api.mvc.Controller
import controllers.Assets

object Developer extends Controller{

  def at(path:String,file:String) = Assets.at(path,file)


}

