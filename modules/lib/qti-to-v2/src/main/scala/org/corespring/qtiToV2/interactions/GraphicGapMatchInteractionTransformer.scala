package org.corespring.qtiToV2.interactions

import play.api.libs.json._

import scala.xml._

object GraphicGapMatchInteractionTransformer extends InteractionTransformer {

  private def safeInt(s: String): Int = {
    try {
      s.toInt
    } catch {
      case e: Exception => 0
    }
  }

  private def safeFloat(s: String): Float = {
    try {
      s.toFloat
    } catch {
      case e: Exception => 0
    }
  }

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "graphicGapMatchInteraction" =>
        elem.child.filter(_.label != "simpleChoice").map(n => n.label match {
          case "prompt" => <p class="prompt">{n.child}</p>
          case _ => n
        }) ++ <corespring-graphic-gap-match id={ identifier }></corespring-graphic-gap-match>
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = (qti \\ "graphicGapMatchInteraction")
    .map( node => {

      val componentId = (node \ "@responseIdentifier").text.trim

      def cutPathPrefix(path:String) = path.substring(path.lastIndexOf('/') + 1)

      def correctResponses: Seq[JsString] = {
        val values: Seq[Node] = (responseDeclaration(node, qti) \\ "value").toSeq
        val mappedValues: Seq[NodeSeq] = (responseDeclaration(node, qti) \\ "mapEntry").map(_ \ "@mapKey")
        (values ++ mappedValues).map(n => JsString(n.text.trim))
      }

      def hotspots = {
        def coords(s:String) = {
          val coordsArray = s.split(',').map(safeFloat)
          Json.obj(
            "left" -> coordsArray(0),
            "top" -> coordsArray(1),
            "width" -> (coordsArray(2) - coordsArray(0)),
            "height" -> (coordsArray(3) - coordsArray(1))
          )
        }
        JsArray(((node \\ "associableHotspot").toSeq).map { n =>
          Json.obj(
            "id" -> (n \ "@identifier").text.trim,
            "shape" -> (n \ "@shape").text.trim,
            "coords" -> coords((n \ "@coords").text.trim)
          )
        })
      }

      def choices = JsArray(((node \\ "gapImg").toSeq).map { n =>
        Json.obj(
          "id" -> (n \ "@identifier").text.trim,
          "label" -> s"<img src='${cutPathPrefix((n \ "object" \ "@data").mkString)}' width='${(n \ "object" \ "@width").mkString}' height='${(n \ "object" \ "@height").mkString}' />",
          "matchMax" -> safeInt((n \ "@matchMax").text.trim),
          "matchMin" -> safeInt((n \ "@matchMin").text.trim)
        )
      })

      val json = Json.obj(
        "componentType" -> "corespring-graphic-gap-match",
        "model" -> Json.obj(
          "config" -> Json.obj(
            "shuffle" -> JsBoolean(false),
            "choiceAreaPosition" -> JsString("top"),
            "backgroundImage" -> Json.obj(
              "path" -> JsString(cutPathPrefix((node \ "object" \ "@data").mkString)),
              "width" -> JsNumber(safeInt((node \ "object" \ "@width").mkString)),
              "height" -> JsNumber(safeInt((node \ "object" \ "@height").mkString))
            ),
            "showHotspots" -> JsBoolean(false)
          ),
          "hotspots" -> hotspots,
          "choices" -> choices
        ),
        "feedback" -> Json.obj(
          "correctFeedbackType" -> "default",
          "partialFeedbackType" -> "default",
          "incorrectFeedbackType" -> "default"
        ),
        "correctResponse" -> correctResponses.map { cr =>
          val idHotspotRegex = """([^\s]*) ([^\s]*)""".r
          cr.asOpt[String] match {
            case Some(idHotspotRegex(id, hotspot)) => Json.obj(
              "id" -> id,
              "hotspot" -> hotspot
            )
            case _ => Json.obj()
          }
        }

      )

      componentId -> json

    }).toMap

}
