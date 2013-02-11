package internal.controllers

import controllers.auth.BaseApi
import play.api.mvc.BodyParsers
import play.mvc.BodyParser

object QtiSearch extends BaseApi{

  def qtiSearch() = SecuredAction(p = new BodyParser.FormUrlEncoded){ request =>
    Ok(internal.html.views.qtiSearch())
  }


}
