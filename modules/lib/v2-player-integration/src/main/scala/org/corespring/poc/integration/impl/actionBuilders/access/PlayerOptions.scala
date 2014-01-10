package org.corespring.poc.integration.impl.actionBuilders.access

case class PlayerOptions(itemId: String, sessionId: String)

object PlayerOptions{


  val ANYTHING = PlayerOptions("*", "*")
}


