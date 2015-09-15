package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.{ TextEntryInteractionTransformer => CorespringTextEntryInteractionTransformer, Transformer, InteractionTransformer }
import org.corespring.qtiToV2.transformers.InteractionRuleTransformer
import play.api.libs.json.{ Json, JsObject }

import scala.xml.Node
import scala.xml.transform.RuleTransformer

class TextEntryInteractionTransformer(qti: Node) extends InteractionTransformer {

  override def interactionJs(qti: Node, manifest: Node): Map[String, JsObject] =
    CorespringTextEntryInteractionTransformer(qti).interactionJs(qti, manifest).map {
      case (id, json) => id -> json.deepMerge(Json.obj(
        "feedback" -> Json.obj(
          "correctFeedbackType" -> "default",
          "incorrectFeedbackType" -> "default"),
        "correctResponses" -> Json.obj(
          "feedback" -> Json.obj(
            "type" -> "default",
            "value" -> "Correct!")),
        "incorrectResponses" -> Json.obj(
          "feedback" -> Json.obj(
            "type" -> "default",
            "value" -> "Good try, but the correct answer is <random selection from correct answers>."))))
    }.toMap

  override def transform(node: Node, manifest: Node): Seq[Node] =
    CorespringTextEntryInteractionTransformer(qti).transform(node, manifest)

}

object TextEntryInteractionTransformer extends Transformer {

  def apply(qti: Node) = new TextEntryInteractionTransformer(qti)

  def transform(qti: Node, manifest: Node): Node =
    new InteractionRuleTransformer(new CorespringTextEntryInteractionTransformer(qti)).transform(qti, manifest).head

}
