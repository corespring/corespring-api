package player.controllers

import play.api.mvc.{Action, Controller}

class AssetLoading extends Controller {

  /** Serve the item player js
    * We require 2 parameters to be passed in with this url:
    * orgId and an encrypted options string
    *
    * We decrypt the options and set them as a session cookie so that related calls to @player.controllers.Session
    * will be authenticated.
    * @return
    */
  def itemPlayer = Action{ request => Ok("alert('todo..')")}
}


object AssetLoading extends AssetLoading