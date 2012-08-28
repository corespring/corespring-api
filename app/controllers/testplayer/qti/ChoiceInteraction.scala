package controllers.testplayer.qti

import scala.xml.Node

class ChoiceInteraction(rootElement: Node) {

  val responseIdentifier: String = (rootElement \ "@responseIdentifier").text
  val choiceIdentifier = (rootElement \\ "simpleChoice").map(_.attribute("identifier"))

}
