package player.controllers.qti

import org.corespring.qti.models.{ RenderingMode, QtiItem }
import RenderingMode._
import org.corespring.qti.processors.FeedbackProcessor
import FeedbackProcessor._
import xml.transform.{ RuleTransformer, RewriteRule }
import scala.xml.{ Xhtml, Node, Elem }
import org.corespring.qti.models.QtiItem
import org.corespring.qti.processors.FeedbackProcessor
import player.controllers.qti.rewrites.{TexRewriteRule, NamespaceRewriteRule}

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
    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(processedQti)
    val itemBody = filterFeedbackContent(xmlWithCsFeedbackIds \ "itemBody");
    //TODO remember after refactor, filter feedback content for feedbackBlock and modalFeedback, feedbackInline's have already been done in the interactions
    val qtiXml = <assessmentItem mode={ renderMode.toString }>{ itemBody }</assessmentItem>
    val string = Xhtml.toXhtml(qtiXml)
    NamespaceRewriteRule.removeNamespaces(string)
  }

}
