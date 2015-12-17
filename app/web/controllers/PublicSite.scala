package web.controllers

import play.api.Mode.Mode
import play.api.templates.Html
import play.api.{ Mode }
import play.api.mvc.{ Action, Controller }

class PublicSite(publicSite: String, mode: Mode) extends Controller {

  def redirect = Action { request =>
    if (mode == Mode.Prod) {
      Redirect(publicSite)
    } else {
      Ok(Html(s"""in prod mode redirect to: <a href="$publicSite">the public site</a>"""))
    }
  }
}

