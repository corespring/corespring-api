package org.corespring.v2player.integration.actionBuilders.access

case class PlayerOptions(itemId: String, sessionId: String)

object PlayerOptions{


  val ANYTHING = PlayerOptions("*", "*")
}


