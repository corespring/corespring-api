package org.corespring.player.accessControl.cookies

import org.bson.types.ObjectId
import org.corespring.platform.core.services.{UserServiceWired, UserService}
import org.corespring.player.accessControl.models.RequestedAccess.Mode
import org.corespring.player.accessControl.models.{ RequestedAccess, RenderOptions }
import play.api.libs.json.{Writes, Json}
import play.api.mvc.{ Session, Request }
import play.api.Logger

trait BasePlayerCookieReader[MODE, OPTIONS] {

  private lazy val logger = Logger("org.corespring.player.accessControl.cookies.CookieReader")

  import PlayerCookieKeys._

  def toMode(s:String) : MODE

  def toOptions(json:String) : OPTIONS

  def activeMode[A](request: Request[A]): Option[MODE] = request.session.get(ACTIVE_MODE).map(toMode(_))

  def renderOptions[A](request: Request[A]): Option[OPTIONS] = {
    val out = request.session.get(RENDER_OPTIONS).map(toOptions(_))
    logger.trace(s"renderOptions: $out")
    out
  }

  def orgIdFromCookie[A](request: Request[A]): Option[String] = {
    val out = request.session.get(PlayerCookieKeys.ORG_ID)
    logger.debug(s"orgId: $out")
    out
  }
}

trait BasePlayerCookieWriter[MODE, OPTIONS] {

  def userService : UserService

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc + (kv._1, kv._2))
  }

  def playerCookies(userId: String, providerId: String, userOptions : OPTIONS)(implicit writes : Writes[OPTIONS]): Seq[(String, String)] = userService.getUser(userId, providerId).map {
    u =>
      playerCookies(u.org.orgId, Some(userOptions))
  }.getOrElse(Seq())

  def playerCookies(orgId: ObjectId, options: Option[OPTIONS])(implicit writes : Writes[OPTIONS]): Seq[(String, String)] = Seq(
    PlayerCookieKeys.ORG_ID -> orgId.toString) ++ options.map { ro => (PlayerCookieKeys.RENDER_OPTIONS -> writes.writes(ro).toString) }

  def activeModeCookie[A](mode: MODE)(implicit request: Request[A]): (String, String) = {
    (PlayerCookieKeys.ACTIVE_MODE -> mode.toString)
  }
}

trait PlayerCookieWriter extends BasePlayerCookieWriter[RequestedAccess.Mode.Mode, RenderOptions]{
  def userService: UserService = UserServiceWired
}


trait PlayerCookieReader extends BasePlayerCookieReader[RequestedAccess.Mode.Mode, RenderOptions]{
  def toMode(s: String): Mode.Mode = Mode.withName(s)
  def toOptions(json: String): RenderOptions = Json.parse(json).as[RenderOptions]
}
