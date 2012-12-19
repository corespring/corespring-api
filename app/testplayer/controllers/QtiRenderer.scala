package testplayer.controllers

import xml.{Node, Elem}
import qti.processors.FeedbackProcessor._
import qti.processors.SelectTextInteractionProcessor._
import xml.transform.{RuleTransformer, RewriteRule}
import qti.models.QtiItem

trait QtiRenderer {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /** Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareQti(qti:Elem, printMode : Boolean = false) : String = {
    val processedQti = new RuleTransformer(new PreprocessXml).transform(qti).asInstanceOf[Elem]
    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(processedQti)
    val itemBody = filterFeedbackContent(xmlWithCsFeedbackIds \ "itemBody");
    //TODO remember after refactor, filter feedback content for feedbackBlock and modalFeedback, feedbackInline's have already been done in the interactions
    val qtiXml = <assessmentItem print-mode={printMode.toString}>{itemBody}</assessmentItem>
    removeNamespaces(qtiXml)
  }

//  /** Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
//   */
//  def prepareSelectText(qti:Elem, printMode : Boolean = false) : String = {
//    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(qti)
//    val itemBody = tokenizeSelectText(filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody"))
//    val qtiXml = <assessmentItem print-mode={printMode.toString}>{itemBody}</assessmentItem>
//    removeNamespaces(qtiXml)
//  }

  /** remove the namespaces - Note: this is necessary to support correct rendering in IE8
   */
  private def removeNamespaces(xml: Elem): String = NamespaceRegex.replaceAllIn(xml.mkString, "")
  private class PreprocessXml extends RewriteRule{
    override def transform(node: Node): Seq[Node] = node match {
      case e:Elem => QtiItem.interactionModels.find(i => i.interactionMatch(e)) match {
        case Some(i) => i.preProcessXml(e)
        case None => e
      }
      case other => other
    }
  }
}
