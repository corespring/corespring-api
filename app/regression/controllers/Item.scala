package regression.controllers

import play.api.mvc.{Action, Controller}
import org.bson.types.ObjectId
import player.accessControl.models.{RenderOptions, RequestedAccess}
import common.config.AppConfig
import player.accessControl.cookies.PlayerCookieWriter
import securesocial.core.java.SecureSocial.UserAwareAction

object Item extends Controller with PlayerCookieWriter {

  def Secured[A](username: String, password: String)(action: Action[A]) = Action(action.parser) { request =>
    request.headers.get("Authorization").flatMap { authorization =>
      authorization.split(" ").drop(1).headOption.filter { encoded =>
        new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
          case u :: p :: Nil if u == username && password == p => true
          case _ => false
        }
      }.map(_ => action(request))
    }.getOrElse {
      Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
    }
  }

  def player(orgId: ObjectId, itemId: ObjectId) = Secured("admin", "1234secret") {
    Action { implicit request =>
      val newCookies : Seq[(String,String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.Preview)
      val newSession = sumSession(request.session, newCookies : _*)
      Ok(regression.views.html.player(itemId.toString() + ":0")).withSession(newSession)
    }
  }

}
