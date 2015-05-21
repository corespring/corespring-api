package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.{TextEntryInteractionTransformer => CorespringTextEntryInteractionTransformer, Transformer, InteractionTransformer}
import play.api.libs.json.{Json, JsObject}

import scala.xml.Node
import scala.xml.transform.RuleTransformer

class TextEntryInteractionTransformer(qti: Node) extends InteractionTransformer {

  override def interactionJs(qti: Node): Map[String, JsObject] =
    CorespringTextEntryInteractionTransformer(qti).interactionJs(qti).map {
      case(id, json) => id -> json.deepMerge(Json.obj(
        "feedback" -> Json.obj(
          "correctFeedbackType" -> "default",
          "incorrectFeedbackType" -> "default"
        ),
        "correctResponses" -> Json.obj(
          "feedback" -> Json.obj(
            "type" -> "default",
            "value" -> "Correct!"
          )
        ),
        "incorrectResponses" -> Json.obj(
          "feedback" -> Json.obj(
            "type" -> "default",
            "value" -> "Good try, but the correct answer is <random selection from correct answers>."
          )
        )
      ))}.toMap

  override def transform(node: Node): Seq[Node] = CorespringTextEntryInteractionTransformer(qti).transform(node)

}

object TextEntryInteractionTransformer extends Transformer {

  def apply(qti: Node) = new TextEntryInteractionTransformer(qti)

  def transform(qti: Node): Node =
    new RuleTransformer(new CorespringTextEntryInteractionTransformer(qti)).transform(qti).head

}
