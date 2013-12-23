package org.corespring.qti.processors

import org.corespring.qti.models.FeedbackIdMapEntry
import scala.Some
import scala.xml.transform.{ RewriteRule, RuleTransformer }
import xml._

/**
 * Provides transformations on JSON strings to add/remove csFeedbackIds to feedback elements, as well as validation for
 * feedbackInline elements.
 */
object FeedbackProcessor extends XmlValidator {

  val FEEDBACK_INLINE = "feedbackInline"
  val FEEDBACK_BLOCK = "feedbackBlock"
  val MODAL_FEEDBACK = "modalFeedback"

  private val FEEDBACK_NODE_LABELS = {
    List(FEEDBACK_INLINE, FEEDBACK_BLOCK, MODAL_FEEDBACK)
  }

  val csFeedbackId = "csFeedbackId"
  val identifier = "identifier"
  val outcomeIdentifier = "outcomeIdentifier"

  private val removeResponsesTransformer = new RuleTransformer(
    new RewriteRule {
      override def transform(n: Node): NodeSeq =
        n match {
          case e: Elem if (FEEDBACK_NODE_LABELS.contains(e.label)) =>
            <a/>.copy(attributes = e.attributes.filter(_.key != identifier)).copy(label = e.label)
          case n => n
        }
    })

  /**
   * RewriteRule which removes all csFeedbackId attributes from <feedbackInline> elements.
   */
  private val feedbackIdentifierRemoverRule = new RewriteRule {

    override def transform(node: Node): Seq[Node] = node match {
      case elem: Elem if (FEEDBACK_NODE_LABELS.contains(elem.label)) => remove(elem, csFeedbackId)
      case other => other
    }

    private def remove(n: Elem, key: String): Node = n.copy(attributes = n.attributes.filter(_.key != key))
  }

  /**
   * Adds csFeedbackId attributes to all feedback elements
   */
  //def addFeedbackIds(xml: NodeSeq): NodeSeq = applyRewriteRuleToXml(xml, new FeedbackIdentifierInserter())
  //def addFeedbackIds(xmlString: String): String = addFeedbackIds(XML.loadString(xmlString)).toString
  def addFeedbackIds(elem: Elem): (Elem, Seq[FeedbackIdMapEntry]) = {
    val feedbackMap = addFeedbackIds(elem, FeedbackMap(elem, Seq(), new IdIncrementor))
    (feedbackMap.elem, feedbackMap.mapping)
  }
  private def addFeedbackIds(elem: Elem, feedbackMap: FeedbackMap): FeedbackMap = {
    if (FEEDBACK_NODE_LABELS.contains(elem.label)) {
      val id: Int = feedbackMap.incrementId
      feedbackMap.mapping = feedbackMap.mapping :+ FeedbackIdMapEntry(id.toString, (elem \ "@outcomeIdentifier").text, (elem \ "@identifier").text)
      feedbackMap.elem = elem % Attribute(None, csFeedbackId, Text(id.toString), Null)
      feedbackMap
    } else {
      var innerNodes: Seq[Node] = Seq()
      elem.child.foreach(node => node match {
        case innerElem: Elem =>
          val innerFeedbackMap = addFeedbackIds(innerElem, FeedbackMap(innerElem, Seq(), feedbackMap.incr))
          innerNodes = innerNodes :+ innerFeedbackMap.elem
          feedbackMap.mapping = feedbackMap.mapping ++ innerFeedbackMap.mapping
        case other => innerNodes = innerNodes :+ other
      })
      feedbackMap.elem = Elem(elem.prefix, elem.label, elem.attributes, elem.scope, true, innerNodes: _*)
      feedbackMap
    }
  }
  private case class FeedbackMap(var elem: Elem, var mapping: Seq[FeedbackIdMapEntry], incr: IdIncrementor) {
    def incrementId: Int = incr.increment
  }
  private class IdIncrementor {
    private var id: Int = 0
    def increment: Int = { id = id + 1; id }
  }
  /**
   * adds the csFeedbackIds to elem given the csFeedbackId -> identifier map. returns the same element passed in
   * @param elem
   * @param mapping
   * @return
   */
  def addFeedbackIds(elem: Elem, mapping: Seq[FeedbackIdMapEntry]): Elem = {
    if (FEEDBACK_NODE_LABELS.contains(elem.label)) {
      mapping.find(fime => (fime.outcomeIdentifier == (elem \ "@outcomeIdentifier").text) && (fime.identifier == (elem \ "@identifier").text)).map(_.csFeedbackId) match {
        case Some(id) => elem % Attribute(None, csFeedbackId, Text(id), Null)
        case None => elem
      }
    } else {
      var innerNodes: Seq[Node] = Seq()
      elem.child.foreach(_ match {
        case innerElem: Elem => innerNodes = innerNodes :+ addFeedbackIds(innerElem, mapping)
        case other => innerNodes = innerNodes :+ other
      })
      Elem(elem.prefix, elem.label, elem.attributes, elem.scope, true, innerNodes: _*)
    }
  }

  /**
   * Removes csFeedbackId attributes to all feedback elements
   */
  def removeFeedbackIds(qtiXml: String): String = applyRewriteRuleToXml(qtiXml, feedbackIdentifierRemoverRule)

  def filterFeedbackContent(xml: NodeSeq): NodeSeq = removeResponsesTransformer.transform(xml)

  def validate(xmlString: String): XmlValidationResult = {
    val xml = XML.loadString(xmlString)
    val feedbackNodes = FEEDBACK_NODE_LABELS.foldLeft(NodeSeq.Empty)((nodes, label) => nodes ++ (xml \\ label))
    val badResults = feedbackNodes.filter(feedback => None == feedback.attribute(outcomeIdentifier))
    badResults.isEmpty match {
      case false => {
        // TODO: This needs to capture line information. Possible solution: http://bit.ly/T9iV5k
        XmlValidationResult(Some(badResults.map(node => ExceptionMessage(node.toString())).toList))
      }
      case _ => XmlValidationResult.success
    }
  }

  /**
   * Applies a provided RewriteRule to the XML representation of its data, and returns XML string with the applied
   * transformation.
   */
  private def applyRewriteRuleToXml(xml: NodeSeq, rewriteRule: RewriteRule): NodeSeq =
    new RuleTransformer(rewriteRule).transform(xml)

  private def applyRewriteRuleToXml(xmlString: String, rewriteRule: RewriteRule): String =
    applyRewriteRuleToXml(XML.loadString(xmlString), rewriteRule).toString()

  /**
   * Adds an incrementing csFeedbackId attribute to each <feedbackInline> element.
   *
   * FIXME: It looks like the transform method is called multiple times per node, resulting in higher than desired id
   * values
   */
  private class FeedbackIdentifierInserter(mapping: Map[String, String]) extends RewriteRule {
    override def transform(node: Node): Seq[Node] =
      node match {
        case elem: Elem if (FEEDBACK_NODE_LABELS.contains(elem.label)) => {
          mapping.find(field => field._2 == (elem \ "@identifier").text).map(_._1) match {
            case Some(id) =>
              elem % Attribute(None, csFeedbackId, Text(id), Null)
              elem
            case None =>
          }
          elem
        }
        case other => other
      }

  }
}

