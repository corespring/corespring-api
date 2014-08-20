package org.corespring.qtiToV2.interactions

import scala.xml._

import play.api.libs.json._

object LineInteractionTransformer extends InteractionTransformer {

  override def interactionJs(qti: Node) = (qti \\ "lineInteraction").map(implicit node => {

    val exhibit = booleanFor("locked", default = false)

    /**
     * Correct Response aren't required if locked="true"
     * or noninteractive="true" is an attribute on the node
     */
    val correctResponse: JsObject = {
      try {
        Json.obj("correctResponse" ->
          (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(_.text).mkString(""))
      } catch {
        case e: Throwable => if (exhibit) Json.obj() else throw e
      }
    }

    val main = Json.obj(
      "componentType" -> "corespring-line",
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
          "initialCurve" -> ((node \ "graphcurve").text match {
            case curve: String if curve.nonEmpty => Some(JsString(curve))
            case _ => None
          }),
          "exhibitOnly" -> Some(JsBoolean(exhibit)),
          "showInputs" -> Some(JsBoolean(booleanFor("show-inputs") && !exhibit)),
          "showLabels" -> Some(JsBoolean(booleanFor("show-labels") && !exhibit)))))

    (node \\ "@responseIdentifier").text -> (main ++ correctResponse)
  }).toMap

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "lineInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
      <corespring-line id={ identifier }></corespring-line>
    }
    case _ => node
  }

  private def booleanFor(attribute: String, default: Boolean = true)(implicit node: Node) =
    ((node \\ s"@$attribute").text) match {
      case "true" => true
      case "false" => false
      case _ => default
    }

}