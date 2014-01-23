package org.corespring.v2player.integration.transformers.qti.interactions

import scala.collection.mutable
import play.api.libs.json.{JsString, JsNumber, Json, JsObject}
import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule

class LineInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  private def correctResponse(node: Node) = {
    Json.obj(
      "equation" -> (responseDeclaration(node, qti) \ "correctResponse" \ "value").map(_.text).mkString("")
    )
  }

  private def config(implicit node: Node) = {
    JsObject(
      Seq(
        "domain" -> optForAttr[JsNumber]("domain"),
        "range" -> optForAttr[JsNumber]("range"),
        "scale" -> optForAttr[JsNumber]("scale"),
        "domainLabel" -> optForAttr[JsString]("domain-label"),
        "rangeLabel" -> optForAttr[JsString]("range-label"),
        "tickLabelFrequency" -> optForAttr[JsNumber]("tick-label-frequency"),
        "sigfigs" -> optForAttr[JsNumber]("sigfigs")
      ).filter{ case (_, v) => v.nonEmpty }
       .map{ case (a,b) => (a, b.get) }
    )
  }

  private def component(node: Node) = {
    val identifier = (node \\ "@responseIdentifier").text

    Json.obj(
      "componentType" -> "corespring-line",
      "correctResponse" -> correctResponse(node),
      "model" -> Json.obj(
        "config" -> config(node)
      )
    )
  }

  (qti \\ "lineInteraction").foreach(node => {
    componentJson.put((node \\ "@responseIdentifier").text, component(node))
  })

  override def transform(node: Node): Seq[Node] = node match {
    case elem: Elem if elem.label == "lineInteraction" => {
      val identifier = (elem \ "@responseIdentifier").text
        <corespring-line id={identifier} />
    }
    case _ => node
  }

}
