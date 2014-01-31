package org.corespring.v2player.integration.transformers.qti.interactions

import play.api.libs.json._
import scala.Predef._
import scala.xml._
import scala.IllegalArgumentException

case class FeedbackBlockTransformer(qti: Node) extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = node
  override def interactionJs(qti: Node) = FeedbackBlockTransformer.interactionJs(qti)

}

object FeedbackBlockTransformer {

  val outcomeIdentifier = """responses\.(.+?)\.(.*)""".r
  val outcomeSpecificRegex = "outcome.(.*)".r

  def interactionJs(qti: Node) = (qti \\ "feedbackBlock").map(node => {
    (node \ "@outcomeIdentifier").text match {
      case outcomeIdentifier(id, value) => {
        val outcomeSpecific = value match {
          case outcomeSpecificRegex(responseIdentifier) => true
          case _ => false
        }
        val feedbackId = value match {
          case "value" => s"${id}_feedback"
          case outcomeSpecificRegex(responseIdentifier) => s"${id}_feedback_${responseIdentifier}"
          case _ =>
            throw new IllegalArgumentException(s"Malformed feedbackBlock outcomeIdentifier: ${(node \\ "@outcomeIdentifier").text}")
        }
        feedbackId -> {
          Json.obj(
            "componentType" -> "corespring-feedback-block",
            "target" -> Json.obj("id" -> id),
            "weight" -> 0,
            "feedback" -> (outcomeSpecific match {
              case true => Json.obj(
                "outcome" -> ((node \ "@outcomeIdentifier").text match {
                  case outcomeIdentifier(id, outcomeSpecificRegex(responseIdentifier)) => {
                    Json.obj(
                      responseIdentifier -> Json.obj(
                        "text" -> node.child.mkString,
                        "correct" -> ((node \ "@incorrectResponse").toString != "true")
                      )
                    )
                  }
                  case _ => throw new IllegalStateException("Node previously identified as outcome specific.")
                })
              )
              case false => Json.obj(
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
            })
          )
        }
      }
      case _ =>
        throw new IllegalArgumentException(s"Malformed feedbackBlock outcomeIdentifier: ${(node \\ "@outcomeIdentifier").text}")
    }
  }).toMap

  /**
   * Takes a QTI document rooted at the provided node, removing <feedbackBlock/>s with duplicate outcomeIdentifier
   * attributes and replacing them with a single <corespring-feedback-block/> element.
   */
  def transform(qti: Node): Node = {
    var ids = Set.empty[String]

    def recurse(node: Node): Seq[Node] = node match {
      case e: Elem if (e.label == "feedbackBlock") => {
        val feedbackId = (e \\ "@outcomeIdentifier").text match {
          case outcomeIdentifier(id, value) => {
            value match {
              case "value" => s"${id}_feedback"
              case outcomeSpecificRegex(responseIdentifier) => s"${id}_feedback_${responseIdentifier}"
            }
          }
          case _ => throw new IllegalArgumentException(
            s"outcomeIdentifier ${(e \\ "@outcomeIdentifier").text} does not match ${outcomeIdentifier.toString}")
        }
        ids.contains(feedbackId) match {
          case true => Seq.empty
          case _ => {
            ids = ids + feedbackId
            <corespring-feedback-block id={feedbackId}></corespring-feedback-block>
          }
        }
      }
      case e: Elem => e.copy(child = e.nonEmptyChildren.map(recurse(_).headOption).flatten)
      case _ => node
    }

    recurse(qti).head
  }

}