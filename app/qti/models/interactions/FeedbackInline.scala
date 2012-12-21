package qti.models.interactions

import qti.models.QtiItem
import qti.models.QtiItem.Correctness
import xml.Node
import play.api.libs.json.{JsString, JsObject, JsValue, Writes}

case class FeedbackInline(csFeedbackId: String,
                          outcomeIdentifier: String,
                          identifier: String,
                          content: String,
                          outcomeAttrs:Seq[String],
                          var defaultFeedback: Boolean = false,
                          var incorrectResponse: Boolean = false) {
  def defaultContent(qtiItem: QtiItem): String =
    qtiItem.responseDeclarations.find(_.identifier == outcomeIdentifier) match {
      case Some(rd) =>
        rd.isCorrect(identifier) match {
          case Correctness.Correct => qtiItem.defaultCorrect
          case Correctness.Incorrect => qtiItem.defaultIncorrect
          case _ => ""
        }
      case None => ""
    }

  override def toString = """[FeedbackInline csFeedbackId: %s,  identifier: %s, content:%s ]"""
    .format(csFeedbackId, identifier, content)
}

object FeedbackInline {
  /**
   * if this feedbackInline is within a interaction, responseIdentifier should be pased in
   * otherwise, if the feedbackInline is within itemBody, then the feedbackInline must have an outcomeIdentifier (equivalent to responseIdentifier) which must be parsed
   * @param node
   * @param responseIdentifier
   * @return
   */
  def apply(node: Node, responseIdentifier: Option[String]): FeedbackInline = {

    def isNullOrEmpty(s: String): Boolean = (s == null || s.length == 0)

    if (node.label == "feedbackInline")
      require(!isNullOrEmpty((node \ "@identifier").text),
        "feedbackInline node doesn't have an identifier: " + node)
    val childBody = new StringBuilder
    node.child.map(
      node => childBody.append(node.toString()))
    def contents: String = childBody.toString()
    val outcomeIdentifier = (node \ "@outcomeIdentifier").text;
    val outcomeAttrs = outcomeIdentifier.split("outcome")(1)

    val feedbackInline = responseIdentifier match {
      case Some(ri) => FeedbackInline((node \ "@csFeedbackId").text,
        ri,
        (node \ "@identifier").text,
        contents,
        outcomeAttrs.split('.'),
        (node \ "@defaultFeedback").text == "true",
        (node \ "@incorrectResponse").text == "true")
      case None => FeedbackInline((node \ "@csFeedbackId").text,
        outcomeIdentifier.split('.')(1),
        (node \ "@identifier").text,
        contents.trim,
        outcomeAttrs.split('.'),
        (node \ "@defaultFeedback").text == "true",
        (node \ "@incorrectResponse").text == "true")
    }
    feedbackInline
  }

  implicit object FeedbackInlineWrites extends Writes[FeedbackInline] {
    override def writes(fi: FeedbackInline): JsValue = {
      JsObject(Seq(
        "csFeedbackId" -> JsString(fi.csFeedbackId),
        "responseIdentifier" -> JsString(fi.outcomeIdentifier),
        "identifier" -> JsString(fi.identifier),
        "body" -> JsString(fi.content)
      ))
    }
  }

}
