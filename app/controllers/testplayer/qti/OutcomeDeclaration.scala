package controllers.testplayer.qti

import scala.xml.Node

class OutcomeDeclaration(rootElement: Node) {

  require(rootElement.label == "outcomeDeclaration")

  val identifier: String = (rootElement \ "@identifier").text
  val value: String = (rootElement \ "defaultValue" \ "value").text

}
