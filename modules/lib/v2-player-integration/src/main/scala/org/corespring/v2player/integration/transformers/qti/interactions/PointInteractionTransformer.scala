package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.collection.mutable
import scala.xml.{Elem, Node}
import play.api.libs.json._
import scala.Some
import scala.reflect.ClassTag

class PointInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  private def config(implicit node: Node) = {
    JsObject(
      Seq(
        "maxPoints" -> optForAttr[JsNumber]("max-points"),
        "scale" -> optForAttr[JsNumber]("scale"),
        "domain" -> optForAttr[JsNumber]("domain"),
        "range" -> optForAttr[JsNumber]("range"),
        "sigfigs" -> optForAttr[JsNumber]("sigfigs"),
        "domainLabel" -> optForAttr[JsString]("domain-label"),
        "rangeLabel" -> optForAttr[JsString]("range-label"),
        "tickLabelFrequency" -> optForAttr[JsNumber]("tick-label-frequency"),
        "pointLabels" -> optForAttr[JsString]("point-labels"),
        "maxPoints" -> optForAttr[JsString]("max-points"),
        "showInputs" -> optForAttr[JsString]("show-inputs"),
        "locked" -> ((node \\ "@locked").isEmpty match {
          case true => None
          case _ => Some(JsBoolean(true))
        })
      ).filter{ case (_, v) => v.nonEmpty }
       .map{ case (a,b) => (a, b.get) }
    )
  }

  private def component(node: Node) = {

    val identifier = (node \\ "@responseIdentifier").text

    Json.obj(
      "componentType" -> "corespring-point-intercept",
      "correctResponse" -> ((qti \\ "responseDeclaration").find(n => (n \ "@identifier").text == identifier) match {
        case Some(responseDeclaration) =>
          (responseDeclaration \ "correctResponse" \ "value").map(_.text)
        case _ =>
          throw new IllegalStateException(s"Item did not contain a responseDeclaration for interaction $identifier")
      }),
      "model" -> Json.obj(
        "config" -> config(node)
      )
    )
  }

  (qti \\ "pointInteraction").foreach(node => {
    componentJson.put((node \\ "@responseIdentifier").text, component(node))
  })

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "pointInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
      <corespring-point-intercept id={identifier} />
    }
    case _ => node
  }

}
