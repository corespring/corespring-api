package developer.controllers

import play.api.mvc.Controller
import controllers.Assets
import securesocial.core.SecureSocial

object Developer extends Controller with SecureSocial{

  def at(path:String,file:String) = Assets.at(path,file)


  def organizations = SecuredAction(){ request =>
    Ok(developer.views.html.organizations(request.user))
  }
}

