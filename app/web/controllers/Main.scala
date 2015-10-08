package web.controllers

import common.controllers.session.SessionHandler
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.web.QtiTemplate
import org.corespring.platform.core.models.{ User, Organization }
import org.corespring.player.accessControl.cookies.{ PlayerCookieKeys, PlayerCookieWriter }
import org.corespring.player.accessControl.models.{RequestedAccess, RenderOptions}
import play.api.mvc._
import scala.Some
import securesocial.core.SecuredRequest

object Main extends BaseApi with PlayerCookieWriter with SessionHandler {

  val UserKey = "securesocial.user"
  val ProviderKey = "securesocial.provider"

  def logout(s: Session): Session = {
    s -
      PlayerCookieKeys.renderOptions -
      UserKey -
      ProviderKey
  }

  def index = SecuredAction {
    implicit request: SecuredRequest[AnyContent] =>
      Redirect("tagger/web")
  }

  private def getDbName(uri: Option[String]): (String, String) = uri match {
    case Some(url) => {
      if (!url.contains("@")) {
        val noAt = """mongodb://(.*)/(.*)""".r
        val noAt(server, name) = url
        (server, name)
      } else {
        val withAt = """.*@(.*)/(.*)""".r
        val withAt(server, name) = url
        (server, name)
      }
    }
    case None => ("?", "?")
  }
}

