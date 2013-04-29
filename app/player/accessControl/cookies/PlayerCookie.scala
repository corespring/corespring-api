package player.accessControl.cookies

import models.{UserOrg, User}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.{Session, Request}
import player.accessControl.models.{RenderOptions, RequestedAccess}


trait PlayerCookieWriter {

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc +(kv._1, kv._2))
  }

  def playerCookies(userId: String, providerId: String): Seq[(String, String)] = User.getUser(userId, providerId).map {
    u =>
      u.orgs match {
        case Seq(UserOrg(id, _)) => playerCookies(id)
        case _ => Seq()
      }
  }.getOrElse(Seq())

  def playerCookies(orgId: ObjectId): Seq[(String, String)] = Seq(PlayerCookieKeys.RENDER_OPTIONS -> Json.toJson(RenderOptions.ANYTHING).toString, PlayerCookieKeys.ORG_ID -> orgId.toString)

  def activeModeCookie[A](mode: RequestedAccess.Mode.Mode = RequestedAccess.Mode.Preview)(implicit request: Request[A]): (String, String) = {
    (PlayerCookieKeys.ACTIVE_MODE -> mode.toString)
  }
}

trait PlayerCookieReader {
  def activeMode[A](request: Request[A]): Option[RequestedAccess.Mode.Mode] = request.session.get(PlayerCookieKeys.ACTIVE_MODE).map {
    k => RequestedAccess.Mode.withName(k)
  }

  def renderOptions[A](request: Request[A]): Option[RenderOptions] = request.session.get(PlayerCookieKeys.RENDER_OPTIONS).map {
    json => Json.parse(json).as[RenderOptions]
  }

  def orgIdFromCookie[A](request: Request[A]): Option[String] = request.session.get(PlayerCookieKeys.ORG_ID)
}
