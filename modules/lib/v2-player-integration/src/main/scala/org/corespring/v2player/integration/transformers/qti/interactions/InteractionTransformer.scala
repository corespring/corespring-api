package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.Node
import play.api.libs.json._
import scala.reflect.ClassTag
import scala.Some
import scala.Some
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsBoolean
import scala.Some
import play.api.libs.json.JsNumber
import org.corespring.qti.models.QtiItem
import play.api.libs.json.Json.JsValueWrapper

trait InteractionTransformer extends XMLNamespaceClearer {

  /**
   * Given a node and QTI document, method looks at node's responseIdentifier attribute, and finds a
   * <responseDeclaration/> within the QTI document whose identifier attribute matches.
   */
  def responseDeclaration(node: Node, qti: Node): Node = {
    (node \ "@responseIdentifier") match {
      case empty: Seq[Node] if empty.isEmpty => throw new IllegalArgumentException("Such bad")
      case nonEmpty: Seq[Node] => {
        val identifier = nonEmpty.text
        (qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
          case Some(node) => node
          case _ => throw new IllegalArgumentException(s"QTI does not contain responseDeclaration for $identifier")
        }
      }
    }
  }

  def feedback(node: Node, qti: Node): JsArray = {
    val qtiItem = QtiItem(qti)
    val choiceIds: Seq[Node] = (node \\ "simpleChoice").toSeq ++ (node \\ "inlineChoice").toSeq ++ (node \\ "feedbackInline").toSeq
    val componentId = (node \ "@responseIdentifier").text.trim

    val feedbackObjects : Seq[JsObject] = choiceIds.map{(n: Node) =>

      val id = (n \ "@identifier").text.trim

      val fbInline = qtiItem.getFeedback(componentId, id)

      fbInline.map{ fb =>
        val content = if (fb.defaultFeedback) {
          fb.defaultContent(qtiItem)
        } else {
          fb.content
        }
        Json.obj("value" -> JsString(id), "feedback" -> JsString(content))
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
  def partialObj(fields : (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter{ case (_, v) => v.nonEmpty }.map{ case (a,b) => (a, b.get) })

}
