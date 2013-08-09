package publicsite.controllers

import java.nio.charset.Charset
import org.corespring.common.config.AppConfig
import play.api.Play
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc._
import scala.io.Codec
import org.corespring.player.accessControl.cookies.PlayerCookieWriter
import org.corespring.player.accessControl.models.{RequestedAccess, RenderOptions}


object Application extends Controller with securesocial.core.SecureSocial with PlayerCookieWriter {

  def index = Action {
    Ok(publicsite.views.html.index())
  }

  def contact = Action {
    Ok(publicsite.views.html.contact())
  }

  def educators = UserAwareAction {
    implicit request =>
      request.user match {
        case Some(user) => {

          val newCookies : Seq[(String,String)] = playerCookies(user.identityId.userId, user.identityId.providerId) :+ activeModeCookie(RequestedAccess.Mode.Preview)
          val newSession = sumSession(request.session, newCookies : _*)
          //TODO: Is it safe to assume that the user has access to this content?
          Ok(publicsite.views.html.educators())
            .withSession(newSession)
        }
        case _ => {
          val orgId = AppConfig.demoOrgId
          val newCookies : Seq[(String,String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.Preview)
          val newSession = sumSession(request.session, newCookies : _*)

          Ok(publicsite.views.html.educators())
            .withSession(newSession)
        }
      }
  }

  def empty = Action {
    request => NotFound("This call has been deprecated - use the new player auth mechanism")
  }

  def partnerships = Action {
    Ok(publicsite.views.html.partnerships())
  }

  def about = Action {
    Ok(publicsite.views.html.about())
  }

  def getItems = Action {
    Ok(Json.parse(io.Source.fromFile(Play.getFile("public/public/conf/items.json"))(new Codec(Charset.forName("UTF-8"))).mkString))
  }

  def features = Action {
    Ok(publicsite.views.html.features())
  }
}
