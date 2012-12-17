package testplayer.controllers

import xml.Elem
import qti.processors.FeedbackProcessor._
import qti.processors.SelectTextInteractionProcessor._

trait QtiRenderer {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /** Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareQti(qti:Elem, printMode : Boolean = false) : String = {
    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(qti)
    val itemBody = filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody")
    val qtiXml = <assessmentItem print-mode={printMode.toString}>{itemBody}</assessmentItem>
    removeNamespaces(qtiXml)
  }

  /** Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareSelectText(qti:Elem, printMode : Boolean = false) : String = {
    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(qti)
    val itemBody = tokenizeSelectText(filterFeedbackContent(addOutcomeIdentifiers(xmlWithCsFeedbackIds) \ "itemBody"))
    val qtiXml = <assessmentItem print-mode={printMode.toString}>{itemBody}</assessmentItem>
    removeNamespaces(qtiXml)
  }

  /** remove the namespaces - Note: this is necessary to support correct rendering in IE8
   */
  private def removeNamespaces(xml: Elem): String = NamespaceRegex.replaceAllIn(xml.mkString, "")

}
