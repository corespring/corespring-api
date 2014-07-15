package org.corespring.qtiToV2.interactions

import scala.xml._

import play.api.libs.json._

object LineInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "lineInteraction").map(implicit node => {
    (node \\ "@responseIdentifier").text ->
      Json.obj(
        "componentType" -> "corespring-line",
        "correctResponse" -> (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(_.text).mkString(""),
        "model" -> Json.obj(
          "config" -> partialObj(
            "domain" -> optForAttr[JsNumber]("domain"),
            "range" -> optForAttr[JsNumber]("range"),
            "scale" -> optForAttr[JsNumber]("scale"),
            "domainLabel" -> optForAttr[JsString]("domain-label"),
            "rangeLabel" -> optForAttr[JsString]("range-label"),
            "tickLabelFrequency" -> optForAttr[JsNumber]("tick-label-frequency"),
            "sigfigs" -> optForAttr[JsNumber]("sigfigs"),
            "initialValues" -> ((node \ "graphline" \\ "point") match {
              case empty: Seq[Node] if empty.isEmpty => None
              case nodes: Seq[Node] => Some(JsArray(nodes.map(n => JsString(n.text))))
            }),
            "exhibitOnly" -> Some(JsBoolean(false)),
            "showInputs" -> Some(JsBoolean(true)))))
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "lineInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
      <corespring-line id={ identifier }></corespring-line>
    }
    case _ => node
  }

}