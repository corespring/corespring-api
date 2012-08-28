package controllers.testplayer.qti

import scala.xml.Node

class ModalFeedback(rootElement: Node) extends FeedbackElement(rootElement: Node) {

  require(rootElement.label == "modalFeedback")

}
