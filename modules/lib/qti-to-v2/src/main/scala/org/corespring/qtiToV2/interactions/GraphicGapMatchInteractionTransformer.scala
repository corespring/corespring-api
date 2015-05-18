package org.corespring.qtiToV2.interactions

import play.api.libs.json._

import scala.xml._

object GraphicGapMatchInteractionTransformer extends InteractionTransformer {

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
          val coordsArray = s.split(',').map(_.toFloat)
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
          "label" -> s"<img src='${cutPathPrefix((n \ "object" \ "@data").mkString)}' />",
          "matchMax" -> (n \ "@matchMax").text.trim,
          "matchMin" -> (n \ "@matchMin").text.trim
        )
      })

      val json = Json.obj(
        "componentType" -> "corespring-graphic-gap-match",
        "model" -> Json.obj(
          "config" -> Json.obj(
            "shuffle" -> (node \ "@shuffle").text,
            "backgroundImage" -> JsString(cutPathPrefix((node \ "object" \ "@data").mkString)),
            "showHotspots" -> JsString("show")
          ),
          "hotspots" -> hotspots,
          "choices" -> choices
        ),
        "feedback" -> feedback(node, qti),
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
