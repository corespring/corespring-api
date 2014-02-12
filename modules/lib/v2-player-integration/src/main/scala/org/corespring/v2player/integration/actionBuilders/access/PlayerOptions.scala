package org.corespring.v2player.integration.actionBuilders.access

import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import play.api.libs.json._

case class PlayerOptions(itemId: String,
                         sessionId: Option[String] = None,
                         secure: Boolean = false,
                         expires: Option[Long] = None,
                         mode: Option[String] = None) {

  def allowSessionId(sessionId: String): Boolean = this.sessionId.map {
    s =>
      s == PlayerOptions.STAR || s == sessionId
  }.getOrElse(sessionId == PlayerOptions.STAR)

  def allowItemId(itemId: String): Boolean = this.itemId == PlayerOptions.STAR || this.itemId == itemId

  def allowMode(mode: Mode) = this.mode.map {
    m =>
      m == PlayerOptions.STAR || Mode.withName(m) == mode
  }.getOrElse(true)
}

object PlayerOptions {
  val STAR = "*"
  val ANYTHING = PlayerOptions(STAR, Some(STAR), false)

  def fromJson(s: String) = try {
    optionsFormat.reads(Json.parse(s)) match {
      case JsSuccess(o, _) => Some(o)
      case JsError(errs) => {
        println(errs)
        None
      }
    }
  } catch {
    case e: Throwable => {
      println(e.getMessage)
      None
    }
  }

  implicit val optionsFormat = Json.format[PlayerOptions]
}


