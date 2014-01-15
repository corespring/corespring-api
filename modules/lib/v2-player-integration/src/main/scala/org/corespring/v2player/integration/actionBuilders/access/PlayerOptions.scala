package org.corespring.v2player.integration.actionBuilders.access

import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import play.api.libs.json.{Json, JsValue, Writes}

case class PlayerOptions(itemId: String,
                         sessionId: String,
                         secure: Boolean,
                         expires: Option[Long] = None,
                         mode : Option[String] = None) {

  def allowSessionId(sessionId: String): Boolean = this.sessionId == PlayerOptions.STAR || this.sessionId == sessionId

  def allowItemId(itemId: String): Boolean = this.itemId == PlayerOptions.STAR || this.itemId == itemId

  def allowMode(mode:Mode) = this.mode.map{
    m =>
      m == PlayerOptions.STAR || Mode.withName(m) == mode
  }.getOrElse(true)
}

object PlayerOptions {
  val STAR = "*"
  val ANYTHING = PlayerOptions(STAR, STAR, false)

  def fromJson(s:String) : Option[PlayerOptions] = None

  implicit object Writes extends Writes[PlayerOptions]{
    def writes(o: PlayerOptions): JsValue = {
      Json.obj()
    }
  }
}


