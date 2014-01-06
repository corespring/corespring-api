package org.corespring.qti.models.responses.processing

import org.corespring.qti.models._
import scala.xml.Node
import org.corespring.qti.models.responses.Response
import play.api.libs.json._
import com.scalapeno.rhinos.RhinosJsonSupport

case class ResponseProcessing(
  itemBody: ItemBody, typeOption: Option[String] = None, script: Option[Script] = None) extends RhinosJsonSupport {

  def process(variables: Option[Map[String, Any]] = None, responses: Option[Seq[Response]] = None): Option[(String,Any)] = {
    script match {
      case Some(scriptObj) => {
        val responseVariables: Map[String, JsValue] = responses match {
          case Some(responses: Seq[Response]) => {
            responses.map(response => itemBody.interactions.find(_.responseIdentifier == response.id) match {
              case Some(interaction) =>
                interaction.toJs(response) match {
                  case Some((script:String,jsValue: JsValue)) => Some(response.id -> jsValue)
                  case _ => None
                }
              case _ => None
            }).flatten.toMap
          }
          case _ => Map.empty
        }

        scriptObj.execute(variables match {
          case Some(v) =>
            deepMerge(responseVariables, v.collect { case (key, value) => (key, toJsValue(value)) }.toMap)
          case _ => responseVariables
        })
      }
      case _ => None
    }
  }

  private def deepMerge(target: Map[String, JsValue], src: Map[String, JsValue]): Map[String, JsValue] = {
    (target.keySet ++ src.keySet).foldLeft(Map.empty[String, JsValue])((map, key) => {
      if (target.keySet.contains(key) && !src.keySet.contains(key)) {
        map + (key -> target.get(key).get)
      } else if (!target.keySet.contains(key) && src.keySet.contains(key)) {
        map + (key -> src.get(key).get)
      } else {
        target.get(key) match {
          case Some(jsObject: JsObject) => map + (key -> jsObject.deepMerge(src.get(key).get.asInstanceOf[JsObject]))
          case Some(jsArray: JsArray) => map + (key -> (jsArray ++ src.get(key).get.asInstanceOf[JsArray]))
          case _ => map + (key -> target.get(key).get)
        }
      }
    }).toMap
  }
}

object ResponseProcessing {
  def apply(itemBody: ItemBody, node: Node): ResponseProcessing = {
    (node \ "@type").text match {
      case tipe: String if tipe == "script" =>
        ResponseProcessing(
          itemBody = itemBody, typeOption = Some((node \ "@type").text), Some(Script((node \ "script").head)))
      case _ => ResponseProcessing(itemBody)
    }
  }
}

