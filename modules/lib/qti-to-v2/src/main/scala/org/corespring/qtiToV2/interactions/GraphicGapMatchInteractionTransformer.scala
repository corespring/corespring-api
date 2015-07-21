package org.corespring.qtiToV2.interactions

import org.corespring.qtiToV2.utils.NumberParsers
import play.api.libs.json._

import scala.xml._

object GraphicGapMatchInteractionTransformer extends InteractionTransformer with NumberParsers {

  override def transform(node: Node): Seq[Node] = {
    val identifier = (node \ "@responseIdentifier").text
    node match {
      case elem: Elem if elem.label == "graphicGapMatchInteraction" =>
        elem.child.filter(_.label != "simpleChoice").map(n => n.label match {
          case "prompt" => <p class="prompt">
                             { n.child }
                           </p>
          case _ => n
        }) ++ <corespring-graphic-gap-match id={ identifier }></corespring-graphic-gap-match>
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = (qti \\ "graphicGapMatchInteraction")
    .map(node => {

      val componentId = (node \ "@responseIdentifier").text.trim

      def cutPathPrefix(path: String) = path.substring(path.lastIndexOf('/') + 1)

      def correctResponses: Seq[JsString] = {
        val values: Seq[Node] = (responseDeclaration(node, qti) \\ "value").toSeq
        val mappedValues: Seq[NodeSeq] = (responseDeclaration(node, qti) \\ "mapEntry").map(_ \ "@mapKey")
        (values ++ mappedValues).map(n => JsString(n.text.trim))
      }

      def hotspots = {
        def coords(shape: String, s: String) = {
          val coordsArray = s.split(',').map(s => floatValueOrZero(s))
          shape match {
            case "rect" => Json.obj(
              "left" -> coordsArray(0),
              "top" -> coordsArray(1),
              "width" -> Math.abs(coordsArray(2) - coordsArray(0)),
              "height" -> Math.abs(coordsArray(3) - coordsArray(1)))
            case "poly" =>
              def xCoords = coordsArray.zipWithIndex.collect { case (x, i) if i % 2 == 0 => x }
              def yCoords = coordsArray.zipWithIndex.collect { case (x, i) if i % 2 == 1 => x }
              def coordPairs = xCoords.zip(yCoords)
              JsArray(coordPairs.map(p => Json.obj("x" -> p._1, "y" -> p._2)))
          }
        }
        JsArray(((node \\ "associableHotspot").toSeq).map { n =>
          val shape = (n \ "@shape").text.trim
          Json.obj(
            "id" -> (n \ "@identifier").text.trim,
            "shape" -> shape,
            "coords" -> coords(shape, (n \ "@coords").text.trim))
        })
      }

      def choices = JsArray(((node \\ "gapImg").toSeq).map { n =>
        Json.obj(
          "id" -> (n \ "@identifier").text.trim,
          "label" -> s"<img src='${cutPathPrefix((n \ "object" \ "@data").mkString)}' width='${(n \ "object" \ "@width").mkString}' height='${(n \ "object" \ "@height").mkString}' />",
          "matchMax" -> intValueOrZero((n \ "@matchMax").text.trim),
          "matchMin" -> intValueOrZero((n \ "@matchMin").text.trim))
      })

      val json = Json.obj(
        "componentType" -> "corespring-graphic-gap-match",
        "model" -> Json.obj(
          "config" -> Json.obj(
            "shuffle" -> false,
            "choiceAreaPosition" -> "left",
            "backgroundImage" -> Json.obj(
              "path" -> cutPathPrefix((node \ "object" \ "@data").mkString),
              "width" -> JsNumber(intValueOrZero((node \ "object" \ "@width").mkString)),
              "height" -> JsNumber(intValueOrZero((node \ "object" \ "@height").mkString))),
            "showHotspots" -> JsBoolean(false)),
          "hotspots" -> hotspots,
          "choices" -> choices),
        "feedback" -> Json.obj(
          "correctFeedbackType" -> "default",
          "partialFeedbackType" -> "default",
          "incorrectFeedbackType" -> "default"),
        "correctResponse" -> correctResponses.map { cr =>
          val idHotspotRegex = """([^\s]*) ([^\s]*)""".r
          cr.asOpt[String] match {
            case Some(idHotspotRegex(id, hotspot)) => Json.obj(
              "id" -> id,
              "hotspot" -> hotspot)
            case _ => Json.obj()
          }
        })

      componentId -> json

    }).toMap

}
