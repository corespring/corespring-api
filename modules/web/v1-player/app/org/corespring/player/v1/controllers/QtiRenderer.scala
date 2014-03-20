package org.corespring.player.v1.controllers

import org.corespring.player.v1.controllers.rewrites.{TexRewriteRule, NamespaceRewriteRule}
import org.corespring.qti.models.RenderingMode._
import org.corespring.qti.processors.FeedbackProcessor
import scala.xml.{ Xhtml, Elem }
import xml.transform.RuleTransformer

trait QtiRenderer {


  val xmlRewrites = Seq(
    NamespaceRewriteRule,
    TexRewriteRule
  )

  /**
   * Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareQti(qti: Elem, renderMode: RenderingMode = Web): String = {
    val processedQti = new RuleTransformer(xmlRewrites: _*).transform(qti).asInstanceOf[Elem]
    val (xmlWithCsFeedbackIds, _) = FeedbackProcessor.addFeedbackIds(processedQti)
    val itemBody = FeedbackProcessor.filterFeedbackContent(xmlWithCsFeedbackIds \ "itemBody")
    //TODO remember after refactor, filter feedback content for feedbackBlock and modalFeedback, feedbackInline's have already been done in the interactions
    val qtiXml = <assessmentItem mode={ renderMode.toString }>{ itemBody }</assessmentItem>
    val string = Xhtml.toXhtml(qtiXml)
    NamespaceRewriteRule.removeNamespaces(string)
  }
}
