package controllers.testplayer.qti
import scala.xml.Node

class FeedbackInline(rootElement: Node) extends FeedbackElement(rootElement: Node) {

  require(rootElement.label == "feedbackInline")

}
