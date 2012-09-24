package controllers.testplayer.qti

import scala.xml.{Elem, Node}
import play.api.libs.json.{JsString, JsObject, Writes}

object FeedbackElement {
  implicit object FeedbackElementWrites extends Writes[FeedbackElement] {
    def writes(feedbackElement: FeedbackElement) = JsObject(
      Seq(
        "csFeedbackId" -> JsString(feedbackElement.csFeedbackId),
        "body" -> JsString(feedbackElement.body),
        "contents" -> JsString(feedbackElement.contents)
      )
    )
  }
}

class FeedbackElement(rootElement: Node) {

  val outcomeIdentifier: String = (rootElement \ "@outcomeIdentifier").text
  val identifier: String = (rootElement \ "@identifier").text

  val csFeedbackId: String = (rootElement \ "@csFeedbackId").text
  val show: Boolean = (rootElement \ "@showHide").text.trim.toLowerCase.equals("show")

  def matches(outcomeIdentifier: String, identifier: String) =
    this.outcomeIdentifier.equals(outcomeIdentifier) && this.identifier.equals(identifier)

  def body: String = rootElement.toString()

  val childBody = new StringBuilder
  rootElement.child.map(node => childBody.append(node.toString()))
  def contents: String = childBody.toString()
}
