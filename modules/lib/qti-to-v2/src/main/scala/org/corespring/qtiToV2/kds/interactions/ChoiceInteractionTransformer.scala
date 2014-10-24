package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.{ChoiceInteractionTransformer => CorespringChoiceInteractionTransformer, InteractionTransformer}
import org.corespring.qtiToV2.kds.XHTMLCleaner
import play.api.libs.json._

import scala.xml.Node

object ChoiceInteractionTransformer extends InteractionTransformer with XHTMLCleaner {

  override def transform(node: Node) = CorespringChoiceInteractionTransformer.transform(node)

  override def interactionJs(qti: Node) = CorespringChoiceInteractionTransformer.interactionJs(qti).map {
    case(id, json) => id -> json.deepMerge(Json.obj(
      "model" -> Json.obj("choices" -> (json \ "model" \ "choices").asOpt[Seq[JsObject]].map(_.map(choice => {
        choice.deepMerge(
          partialObj("rationale" -> (choice \ "value").asOpt[String]
            .map(choiceId => rationale(qti, id, choiceId)).flatten))
      })).getOrElse(throw new Exception(s"choiceInteraction $id was missing choices")))))
    }.toMap


  private def rationale(qti: Node, id: String, choiceId: String): Option[JsString] =
    (qti \\ "choiceRationales").find(c => (c \ "@responseIdentifier").text == id)
      .map(c => (c \\ "rationale").find(r => (r \ "@identifier").text == choiceId)).flatten
      .map(n => JsString(n.child.mkString.cleanWhitespace))

}
