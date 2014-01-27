package org.corespring.v2player.integration.transformers.qti.interactions

import play.api.libs.json._
import scala.Predef._
import scala.xml._

case class FeedbackBlockTransformer(qti: Node) extends InteractionTransformer {

  var usedFeedbackIds = Set.empty[String]

  override def transform(node: Node): Seq[Node] = {
    node match {
      case e: Elem if e.label == "feedbackBlock" => {
        (e \ "@outcomeIdentifier").text match {
          case FeedbackBlockTransformer.outcomeIdentifier(id) if (!usedFeedbackIds.contains(id)) => {
            usedFeedbackIds = usedFeedbackIds + id
            <corespring-feedback-block id={s"${id}_feedback"}/>
          }
          case _ => Seq.empty
        }
      }
      case _ => node
    }
  }

  override def interactionJs(qti: Node) = FeedbackBlockTransformer.interactionJs(qti)

}

object FeedbackBlockTransformer {

  val outcomeIdentifier = """responses\.(.+?)\..*""".r

  def interactionJs(qti: Node) = (qti \\ "feedbackBlock").map(node => {
    (node \ "@outcomeIdentifier").text match {
      case outcomeIdentifier(id) => {
        s"${id}_feedback" -> {
          Json.obj(
            "componentType" -> "corespring-feedback-block",
            "target" -> Json.obj("id" -> id),
            "feedback" -> Json.obj(
              "correct" -> JsObject(
                (qti \\ "feedbackBlock").filter(n => (n \ "@incorrectResponse").toString != "true").map(feedbackBlock => {
                  (feedbackBlock \ "@identifier").text match {
                    case "" => "*" -> JsString(feedbackBlock.child.text.trim)
                    case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
                  }
                })
              ),
              "incorrect" -> JsObject(
                (qti \\ "feedbackBlock").filter(n => (n \ "@incorrectResponse").toString == "true").map(feedbackBlock => {
                  (feedbackBlock \ "@identifier").text match {
                    case "" => "*" -> JsString(feedbackBlock.child.text.trim)
                    case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
                  }
                })
              )
            )
          )
        }
      }
      case _ =>
        throw new IllegalArgumentException(s"Malformed feedbackBlock outcomeIdentifier: ${(node \\ "@outcomeIdentifier").text}")
    }
  }).toMap

}