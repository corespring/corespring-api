package org.corespring.v2player.integration.transformers.qti.interactions

import scala.collection.mutable
import play.api.libs.json._
import scala.xml.{Elem, Node}
import scala.xml.transform.RewriteRule
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

class FocusTaskInteractionTransformer(componentJson: mutable.Map[String, JsObject], qti: Node)
  extends RewriteRule
  with InteractionTransformer {

  (qti \\ "focusTaskInteraction").foreach(implicit node => {
    componentJson.put((node \\ "@responseIdentifier").text,
      Json.obj(
        "componentType" -> "corespring-focus-task",
        "correctResponse" -> Json.obj(
          "value" -> (responseDeclaration(node, qti) \\ "value").map(_.text)
        ),
        "model" -> partialObj(
          "prompt" -> (node \ "prompt").headOption.map(n => JsString(n.text)),
          "config" -> Some(partialObj(
            "shuffle" -> optForAttr[JsBoolean]("shuffle"),
            "itemShape" -> optForAttr[JsString]("itemShape")
          )),
          "choices" -> Some(JsArray(
            (node \\ "focusChoice").map(choiceNode =>
              Json.obj(
                "label" -> choiceNode.text,
                "value" -> (choiceNode \ "@identifier").text
              )
            )
          ))
        )
      )
    )
  })

  override def transform(node: Node): Seq[Node] = node match {
    case e: Elem if e.label == "focusTaskInteraction" => {
      val identifier = (e \ "@responseIdentifier").text
      <corespring-focus-task id={identifier} />
    }
    case _ => node
  }

}
