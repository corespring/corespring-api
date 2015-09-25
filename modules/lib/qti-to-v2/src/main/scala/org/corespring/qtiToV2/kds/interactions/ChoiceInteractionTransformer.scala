package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.{ChoiceInteractionTransformer => CorespringChoiceInteractionTransformer, InteractionTransformer}
import org.corespring.qtiToV2.kds.XHTMLCleaner
import play.api.libs.json._

import scala.xml.{NodeSeq, Node}
import scala.xml.transform.{RewriteRule, RuleTransformer}

object ChoiceInteractionTransformer extends InteractionTransformer with XHTMLCleaner {

  override def transform(node: Node, manifest: Node) = new RuleTransformer(new RewriteRule {
    override def transform(n: Node): NodeSeq = n match {
      case n: Node if (Seq("inlineChoiceRationales", "choiceRationales").contains(n.label)) => Seq.empty
      case n => n
    }
  }).transform(CorespringChoiceInteractionTransformer.transform(node, manifest))

  override def interactionJs(qti: Node, manifest: Node) =
    CorespringChoiceInteractionTransformer.interactionJs(qti, manifest).map {
      case (id, json) => id -> json.deepMerge(partialObj(
        "feedback" -> ((json \ "componentType").asOpt[String] match {
          case Some("corespring-inline-choice") =>
            Some(JsArray((json \ "model" \ "choices").as[Seq[JsObject]].map { choice =>
              Json.obj("value" -> (choice \ "value").as[String], "feedbackType" -> "default")
            }))
          case _ => None
        }),
        "rationales" -> Some(JsArray((json \ "model" \ "choices").as[Seq[JsObject]].map(c => Json.obj(
          "choice" -> Some(c \ "value"),
          "rationale" -> rationale(qti, id, (c \ "value").as[String])
        )))),
        "model" -> Some(Json.obj("shuffle" -> false)))
      )
    }

  private def rationale(qti: Node, id: String, choiceId: String): Option[JsString] =
    (qti \\ "choiceRationales" ++ qti \\ "inlineChoiceRationales").find(c => (c \ "@responseIdentifier").text == id)
      .map(c => (c \\ "rationale").find(r => (r \ "@identifier").text == choiceId)).flatten
      .map(n => JsString(n.child.mkString.cleanWhitespace))

}
