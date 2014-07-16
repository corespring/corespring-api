package org.corespring.qtiToV2.interactions

import org.corespring.qti.models.QtiItem

import scala.reflect.ClassTag
import scala.xml.Node
import scala.xml.transform.RewriteRule

import play.api.libs.json._

abstract class InteractionTransformer extends RewriteRule with XMLNamespaceClearer {

  def interactionJs(qti: Node): Map[String, JsObject]

  /**
   * Given a node and QTI document, method looks at node's responseIdentifier attribute, and finds a
   * <responseDeclaration/> within the QTI document whose identifier attribute matches.
   */
  def responseDeclaration(node: Node, qti: Node): Node = {
    (node \ "@responseIdentifier").text match {
      case "" => throw new IllegalArgumentException("Node does not have a responseIdentifier")
      case identifier: String => {
        (qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
          case Some(node) => node
          case _ => throw new IllegalArgumentException(s"QTI does not contain responseDeclaration for $identifier")
        }
      }
    }
  }

  def feedback(node: Node, qti: Node): JsArray = {
    val qtiItem = QtiItem(qti)
    val choiceIds: Seq[Node] = Seq("simpleChoice", "inlineChoice", "feedbackInline").map(node \\ _).map(_.toSeq).flatten
    val componentId = (node \ "@responseIdentifier").text.trim

    val feedbackObjects: Seq[JsObject] = choiceIds.map { (n: Node) =>

      val id = (n \ "@identifier").text.trim
      val fbInline = qtiItem.getFeedback(componentId, id)

      fbInline.map { fb =>
        val content = if (fb.defaultFeedback) {
          fb.defaultContent(qtiItem)
        } else {
          fb.content
        }
        Json.obj("value" -> id, "feedback" -> content, "notChosenFeedback" -> content)
      }
    }.flatten.distinct
    JsArray(feedbackObjects)
  }

  /**
   * Returns an Option of JsValue subtype T for an attribute of the implicit node. For example:
   *
   *   implicit val node = <span class="great" count=2 awesome=true>Test</span>
   *
   *   optForAttr[JsString]("class")    // Some(JsString(great))
   *   optForAttr[JsNumber]("count")    // Some(JsNumber(2))
   *   optForAttr[JsBoolean]("awesome") // Some(JsBoolean(true))
   *   optForAttr[JsString]("id")       // None
   */
  def optForAttr[T <: JsValue](attr: String)(implicit node: Node, mf: ClassTag[T]) = {
    (node \ s"@$attr") match {
      case empty: Seq[Node] if empty.isEmpty => None
      case nonEmpty: Seq[Node] if (classOf[JsNumber] isAssignableFrom mf.runtimeClass) =>
        Some(JsNumber(BigDecimal(nonEmpty.head.text)))
      case nonEmpty: Seq[Node] if (classOf[JsBoolean] isAssignableFrom mf.runtimeClass) =>
        Some(JsBoolean(nonEmpty.head.text.toBoolean))
      case nonEmpty: Seq[Node] => Some(JsString(nonEmpty.head.text.toString))
    }
  }

  /**
   * Returns a JsObject with only the fields whose values are Some(JsValue)
   */
  def partialObj(fields: (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter { case (_, v) => v.nonEmpty }.map { case (a, b) => (a, b.get) })

}

trait Transformer {

  def transform(qti: Node): Node

}