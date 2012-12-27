package qti.models.interactions

import xml._
import scala.Null
import transform.{RuleTransformer, RewriteRule}
import xml.Null
import scala.Null
import xml.Text
import scala.Some
import models.FeedbackIdMapEntry


object InteractionProcessing {
  val FEEDBACK_INLINE = "feedbackInline"
  val FEEDBACK_BLOCK = "feedbackBlock"
  val MODAL_FEEDBACK = "modalFeedback"

  private val FEEDBACK_NODE_LABELS = {
    List(FEEDBACK_INLINE, FEEDBACK_BLOCK, MODAL_FEEDBACK)
  }

  val csFeedbackId = "csFeedbackId"
  val identifier = "identifier"
  val outcomeIdentifier = "outcomeIdentifier"

  class FeedbackOutcomeIdentifierInserter(ci:InteractionWithChoices) extends RewriteRule{
    private def addIdentifiersToElem(elem: Elem, outcomeIdentifier: String, identifier: String): Elem = {
      var elementWithIdentifiers = elem
      if ((elem \ "@identifier").isEmpty) {
        elementWithIdentifiers = elementWithIdentifiers % Attribute(None, identifier, Text(identifier), Null)
      }
      if ((elem \ "@outcomeIdentifier").isEmpty) {
        elementWithIdentifiers = elementWithIdentifiers % Attribute(None, outcomeIdentifier, Text("responses.%s.value".format(outcomeIdentifier)), Null)
      }
      elementWithIdentifiers
    }
    override def transform(node: Node): Seq[Node] = node match {
      case elem: Elem => {
        if (elem.label equals FEEDBACK_INLINE) {
          elem.attribute(csFeedbackId) match {
            case Some(csFeedbackId) => {
              val optIdentifiers:Option[(String,String)] = ci.choices.find(_.feedbackInline.exists(_.csFeedbackId == csFeedbackId.text)) match {
                case Some(sc) => Some(ci.responseIdentifier -> sc.identifier)
                case None => None
              }
              optIdentifiers match {
                case Some(identifiers) => addIdentifiersToElem(elem, identifiers._1, identifiers._2)
                case None => elem
              }
            }
            case None => elem
          }
        }else {
          elem
        }
      }
      case other => other
    }
  }
}
