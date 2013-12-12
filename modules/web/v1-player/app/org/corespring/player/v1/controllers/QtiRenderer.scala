package org.corespring.player.v1.controllers

import org.corespring.qti.models.QtiItem
import org.corespring.qti.models.RenderingMode._
import scala.Some
import scala.xml.{ Xhtml, Node, Elem }
import xml.transform.{ RuleTransformer, RewriteRule }
import org.corespring.qti.processors.FeedbackProcessor

trait QtiRenderer {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /**
   * Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareQti(qti: Elem, renderMode: RenderingMode = Web): String = {
    val processedQti = new RuleTransformer(new PreprocessXml).transform(qti).asInstanceOf[Elem]
    val (xmlWithCsFeedbackIds, _) = FeedbackProcessor.addFeedbackIds(processedQti)
    val itemBody = FeedbackProcessor.filterFeedbackContent(xmlWithCsFeedbackIds \ "itemBody")
    //TODO remember after refactor, filter feedback content for feedbackBlock and modalFeedback, feedbackInline's have already been done in the interactions
    val qtiXml = <assessmentItem mode={ renderMode.toString }>{ itemBody }</assessmentItem>
    val string = Xhtml.toXhtml(qtiXml)
    removeNamespaces(string)
  }

  /**
   * remove the namespaces - Note: this is necessary to support correct rendering in IE8
   */
  private def removeNamespaces(xml: String): String = NamespaceRegex.replaceAllIn(xml, "")
  private class PreprocessXml extends RewriteRule {
    override def transform(node: Node): Seq[Node] = node match {
      case e: Elem => QtiItem.interactionModels.find(i => i.interactionMatch(e)) match {
        case Some(i) => i.preProcessXml(e)
        case None => e
      }
      case other => other
    }
  }
}
