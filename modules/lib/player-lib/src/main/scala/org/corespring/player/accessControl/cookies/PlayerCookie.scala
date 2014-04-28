package org.corespring.player.accessControl.cookies

import org.bson.types.ObjectId
import org.corespring.platform.core.services.{ UserServiceWired, UserService }
import org.corespring.player.accessControl.models.RequestedAccess.Mode
import org.corespring.player.accessControl.models.{ RequestedAccess, RenderOptions }
import play.api.libs.json.{ Writes, Json }
import play.api.mvc.{ RequestHeader, Session, Request }
import play.api.Logger

trait CookieKeys {
  def renderOptions: String
  def orgId: String
  def activeMode: String
}

trait BasePlayerCookieReader[MODE, OPTIONS] {

  def keys: CookieKeys

  def toMode(s: String): MODE

  def toOptions(json: String): OPTIONS

  def activeMode(request: RequestHeader): Option[MODE] = request.session.get(keys.activeMode).map(toMode(_))

  def renderOptions(request: RequestHeader): Option[OPTIONS] = {
    val out = request.session.get(keys.renderOptions).map(toOptions(_))
    out
  }

  def orgIdFromCookie(request: RequestHeader): Option[String] = {
    val out = request.session.get(keys.orgId)
    out
  }
}

trait BasePlayerCookieWriter[MODE, OPTIONS] {

  def userService: UserService

  def keys: CookieKeys

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc + (kv._1, kv._2))
  }

  def playerCookies(userId: String, providerId: String, userOptions: OPTIONS)(implicit writes: Writes[OPTIONS]): Seq[(String, String)] = userService.getUser(userId, providerId).map {
    u =>
      playerCookies(u.org.orgId, Some(userOptions))
  }.getOrElse(Seq())

  def playerCookies(orgId: ObjectId, options: Option[OPTIONS])(implicit writes: Writes[OPTIONS]): Seq[(String, String)] = Seq(
    keys.orgId -> orgId.toString) ++ options.map { ro => (keys.renderOptions -> writes.writes(ro).toString) }

  def activeModeCookie[A](mode: MODE)(implicit request: Request[A]): (String, String) = {
    (keys.activeMode -> mode.toString)
  }
}

trait PlayerCookieWriter extends BasePlayerCookieWriter[RequestedAccess.Mode.Mode, RenderOptions] {
  def userService: UserService = UserServiceWired
  def keys = PlayerCookieKeys
}

trait PlayerCookieReader extends BasePlayerCookieReader[RequestedAccess.Mode.Mode, RenderOptions] {
  def toMode(s: String): Mode.Mode = Mode.withName(s)
  def toOptions(json: String): RenderOptions = Json.parse(json).as[RenderOptions]
  def keys = PlayerCookieKeys
}