package org.corespring.v2player.integration.actionBuilders.access

case class PlayerOptions(
                          itemId: String,
                          sessionId: String,
                          secure: Boolean) {
  def allowSessionId(sessionId: String): Boolean = this.sessionId == PlayerOptions.STAR || this.sessionId == sessionId

  def allowItemId(itemId: String): Boolean = this.itemId == PlayerOptions.STAR || this.itemId == itemId
}

object PlayerOptions {
  val STAR = "*"
  val ANYTHING = PlayerOptions(STAR, STAR, false)
}


