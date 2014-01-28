package org.corespring.v2player.integration.transformers.qti.interactions

import play.api.libs.json._
import scala.Predef._
import scala.xml._

case class FeedbackBlockTransformer(qti: Node) extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = node
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

  /**
   * Takes a QTI document rooted at the provided node, removing <feedbackBlock/>s with duplicate outcomeIdentifier
   * attributes and replacing them with a single <corespring-feedback-block/> element.
   */
  def transform(qti: Node): Node = {
    var ids = Set.empty[String]

    def recurse(node: Node): Seq[Node] = {

      node match {
        case e: Elem if (e.label == "feedbackBlock") => {
          val id = (e \\ "@outcomeIdentifier").text match {
            case outcomeIdentifier(id) => id
            case _ => throw new IllegalArgumentException(
              s"outcomeIdentifier ${(e \\ "@outcomeIdentifier").text} does not match ${outcomeIdentifier.toString}")
          }
          ids.contains(id) match {
            case true => Seq.empty
            case _ => {
              ids = ids + id
              <corespring-feedback-block id={s"${id}_feedback"}></corespring-feedback-block>
            }
          }
        }
        case e: Elem => e.copy(child = e.nonEmptyChildren.map(recurse(_).headOption).flatten)
        case _ => node
      }
    }

    recurse(qti).head
  }

}