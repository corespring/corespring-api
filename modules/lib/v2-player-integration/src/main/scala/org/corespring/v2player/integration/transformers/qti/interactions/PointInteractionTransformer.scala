package org.corespring.v2player.integration.transformers.qti.interactions

import scala.xml._
import play.api.libs.json._
import scala.Some

object PointInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "pointInteraction").map(implicit node => {
    val identifier = (node \\ "@responseIdentifier").text
    identifier -> Json.obj(
      "componentType" -> "corespring-point-intercept",
      "correctResponse" -> (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(_.text),
      "model" -> Json.obj(
        "config" -> partialObj(
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
        )
      )
    )
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "pointInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
      <corespring-point-intercept id={identifier} />
    }
    case _ => node
  }

}
