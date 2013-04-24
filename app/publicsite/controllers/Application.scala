package publicsite.controllers

import java.nio.charset.Charset
import models.Organization
import org.bson.types.ObjectId
import play.api.Play
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc._
import player.rendering.{PlayerCookieKeys, PlayerCookieWriter}
import scala.io.Codec
import player.controllers.auth.RequestedAccess


object Application extends Controller with securesocial.core.SecureSocial with PlayerCookieWriter {

  def index = Action {
    Ok(publicsite.views.html.index())
  }
  def contact = Action {
    Ok(publicsite.views.html.contact())
  }

  def educators = UserAwareAction { implicit request =>
    request.user match {
      case Some(user) => {

        val newSession = request.session +
          activeMode2(RequestedAccess.PREVIEW_MODE) +
          orgIdCookie(user.id.id,user.id.providerId) +
          renderOptionsCookie
        //val newSession = request.session + activeMode2(RequestedAccess.PREVIEW_MODE) + renderOptions()
        //TODO: Is it safe to assume that the user has access to this content?
        Ok(publicsite.views.html.educators())
          .withSession(newSession)
      }
      case _ => {
        val orgId = new ObjectId(Organization.CORESPRING_ORGANIZATION_ID)

        val newSession = request.session +
          activeMode2(RequestedAccess.PREVIEW_MODE) +
          orgIdCookie(orgId) +
          renderOptionsCookie

        Ok(publicsite.views.html.educators())
          .withSession(newSession)
      }
    }
  }

  def empty = Action{ request => NotFound("This call has been deprecated - use the new player auth mechanism") }

  def partnerships = Action {
    Ok(publicsite.views.html.partnerships())
  }
  def about = Action {
    Ok(publicsite.views.html.about())
  }
  def getItems = Action {
    Ok(Json.parse(io.Source.fromFile(Play.getFile("public/public/conf/items.json"))(new Codec(Charset.forName("UTF-8"))).mkString))
  }
}
