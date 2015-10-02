package web.controllers

import common.controllers.session.SessionHandler
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.models.web.QtiTemplate
import org.corespring.models.{ User, Organization }
import org.corespring.player.accessControl.cookies.{ PlayerCookieKeys, PlayerCookieWriter }
import org.corespring.player.accessControl.models.{ RequestedAccess, RenderOptions }
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

      val uri: Option[String] = play.api.Play.current.configuration.getString("mongodb.default.uri")
      val (dbServer, dbName) = getDbName(uri)
      val userId = request.user.identityId
      val user: User = User.getUser(request.user.identityId).getOrElse(throw new RuntimeException("Unknown user"))
      Organization.findOneById(user.org.orgId) match {
        case Some(userOrg) => Ok(web.views.html.index(QtiTemplate.findAll().toList, dbServer, dbName, user, userOrg))
          .withSession(
            sumSession(request.session, playerCookies(userId.userId, userId.providerId, RenderOptions.ANYTHING) :+ activeModeCookie(RequestedAccess.Mode.Preview): _*))
        case None => InternalServerError("could not find organization of user")
      }

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
