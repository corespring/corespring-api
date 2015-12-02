package web.controllers

import org.corespring.common.config.AppConfig
import play.api.templates.Html
import play.api.{ Play, Mode }
import play.api.mvc.{ Action, Controller }

object PublicSite extends Controller {

  lazy val publicSite = AppConfig.publicSite

  def redirect = Action { request =>
    if (Play.current.mode == Mode.Prod) {
      Redirect(publicSite)
    } else {
      Ok(Html(s"""in prod mode redirect to: <a href="$publicSite">the public site</a>"""))
    }
  }
}

