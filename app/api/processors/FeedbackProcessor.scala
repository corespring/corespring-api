package api.processors

import xml._
import scala.xml.transform.{RewriteRule, RuleTransformer}
import play.api.libs.json.{JsValue, JsObject, Json}
import controllers.testplayer.qti.xml.{XmlValidationResult, XmlValidator}
import controllers.testplayer.qti.xml.XmlValidationResult.success
import controllers.testplayer.qti.xml.ExceptionMessage
import scala.Some
import models.Item

/**
 * Provides transformations on JSON strings to add/remove csFeedbackIds to feedback elements, as well as validation for
 * feedbackInline elements.
 */
object FeedbackProcessor extends XmlValidator {

  private val FEEDBACK_NODE_LABELS = List("feedbackInline", "modalFeedback")

  private val FEEDBACK_ID_ATTTRIBUTE = "csFeedbackId"
  private val OUTCOME_ID_ATTRIBUTE = "outcomeIdentifier"

  private val removeResponsesTransformer = new RuleTransformer(
    new RewriteRule {
      override def transform(n: Node): NodeSeq =
        n match {
          case e: Elem if (FEEDBACK_NODE_LABELS.contains(e.label)) =>
            e.attribute("csFeedbackId") match {
              case Some(id) => {
                <a csFeedbackId={id}></a>.copy(label = e.label)
              }
              case None => {
                  <a/>.copy(label = e.label)
              }
            }
          case n => n
        }
    })

  /**
   * RewriteRule which removes all csFeedbackId attributes from <feedbackInline> elements.
   */
  private val feedbackIdentifierRemoverRule = new RewriteRule {

    override def transform(node: Node): Seq[Node] = node match {
      case elem: Elem if (FEEDBACK_NODE_LABELS.contains(elem.label)) => {
        remove(elem, FEEDBACK_ID_ATTTRIBUTE)
      }
      case other => other
    }

    // FIXME This breaks if that attribute doesn't exist
    private def remove(n: Elem, key: String): Node = {
      n.copy(attributes = n.attributes.find(_.key == key).get.remove(key))
    }

  }

  /**
   * Adds csFeedbackId attributes to all feedback elements in a JSON string's data field.
   */
  def addFeedbackIds(jsonData: String): String = applyRewriteRuleToJson(jsonData, new FeedbackIdentifierInserter())

  /**
   * Removes csFeedbackId attributes to all feedback elements in a JSON string's data field.
   */
  def removeFeedbackIds(jsonData: String): String = applyRewriteRuleToJson(jsonData, feedbackIdentifierRemoverRule)

  def filterFeedbackContent(xml: NodeSeq) = removeResponsesTransformer.transform(xml)

  def validate(xmlString: String): XmlValidationResult = {
    val xml = XML.loadString(xmlString)
    val feedbackNodes = FEEDBACK_NODE_LABELS.foldLeft(NodeSeq.Empty)((nodes, label) => nodes ++ (xml \\ label))
    val badResults = feedbackNodes.filter(feedback => None == feedback.attribute(OUTCOME_ID_ATTRIBUTE))
    badResults.isEmpty match {
      case false => {
        // TODO: This needs to capture line information. Possible solution: http://bit.ly/T9iV5k
        XmlValidationResult(Some(badResults.map(node => ExceptionMessage(node.toString())).toList))
      }
      case _ => success
    }
  }

  /**
   * Locates a data element in a JSON string, applies a provided RewriteRule to the XML representation of its
   * data, and returns JSON string with the applied transformation.
   *
   * TODO: This can be more functional.
   */
  private def applyRewriteRuleToJson(jsonData: String, rewriteRule: RewriteRule): String = {

    var newJson = jsonData
    val files: Seq[JsObject] = (Json.parse(jsonData) \ "data" \ "files").asOpt[Seq[JsObject]].getOrElse(Seq())
    files.foreach(file => {
      (file \ "content").asOpt[String] match {
        case Some(qtiXml) => {
          val newQtiXml = new RuleTransformer(rewriteRule).transform(XML.loadString(qtiXml)).toString
          newJson = newJson.replace(jsonEncode(qtiXml), jsonEncode(newQtiXml))
        }
        case None => {}
      }
    })
    newJson
  }

  private def jsonEncode(xml: String): String = xml.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n")

  /**
   * Adds an incrementing csFeedbackId attribute to each <feedbackInline> element.
   *
   * FIXME: It looks like the transform method is called multiple times per node, resulting in higher than desired id
   * values
   */
  private class FeedbackIdentifierInserter extends RewriteRule {

    var id: Int = 0

    override def transform(node: Node): Seq[Node] = node match {
      case elem: Elem if (FEEDBACK_NODE_LABELS.contains(elem.label)) => {
        id = id + 1
        elem % Attribute(None, FEEDBACK_ID_ATTTRIBUTE, Text(id.toString), Null)
      }
      case other => other
    }

  }

}



