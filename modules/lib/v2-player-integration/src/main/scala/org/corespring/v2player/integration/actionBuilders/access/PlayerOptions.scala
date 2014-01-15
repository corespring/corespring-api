package org.corespring.v2player.integration.actionBuilders.access

import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import play.api.libs.json._

case class PlayerOptions(itemId: String,
                         sessionId: String,
                         secure: Boolean,
                         expires: Option[Long] = None,
                         mode: Option[String] = None) {

  def allowSessionId(sessionId: String): Boolean = this.sessionId == PlayerOptions.STAR || this.sessionId == sessionId

  def allowItemId(itemId: String): Boolean = this.itemId == PlayerOptions.STAR || this.itemId == itemId

  def allowMode(mode: Mode) = this.mode.map {
    m =>
      m == PlayerOptions.STAR || Mode.withName(m) == mode
  }.getOrElse(true)
}

object PlayerOptions {
  val STAR = "*"
  val ANYTHING = PlayerOptions(STAR, STAR, false)

  def fromJson(s: String) = try {
    optionsFormat.reads(Json.parse(s)) match {
      case JsSuccess(o, _) => Some(o)
      case JsError(errs) => None
    }
  } catch {
    case e: Throwable => None
  }

  implicit val optionsFormat = Json.format[PlayerOptions]
}


