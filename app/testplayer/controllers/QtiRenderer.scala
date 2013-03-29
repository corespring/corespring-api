package testplayer.controllers

import xml.{Node, Elem}
import qti.processors.FeedbackProcessor._
import xml.transform.{RuleTransformer, RewriteRule}
import qti.models.QtiItem
import qti.models.RenderingMode._


trait QtiRenderer {

  val NamespaceRegex = """xmlns.*?=".*?"""".r

  /** Prepare the raw qti xml for rendering. Remove answers and add csFeedbackIds
   */
  def prepareQti(qti:Elem, renderMode : RenderingMode = Web) : String = {
    val processedQti = new RuleTransformer(new PreprocessXml).transform(qti).asInstanceOf[Elem]
    val (xmlWithCsFeedbackIds, _) = addFeedbackIds(processedQti)
    val itemBody = filterFeedbackContent(xmlWithCsFeedbackIds \ "itemBody");
    //TODO remember after refactor, filter feedback content for feedbackBlock and modalFeedback, feedbackInline's have already been done in the interactions
    val qtiXml = <assessmentItem mode={renderMode.toString}>{itemBody}</assessmentItem>
    removeNamespaces(qtiXml)
  }

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
