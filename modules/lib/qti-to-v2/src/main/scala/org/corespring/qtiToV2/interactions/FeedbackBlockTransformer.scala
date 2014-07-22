package org.corespring.qtiToV2.interactions

import scala.Predef._
import scala.xml._

import play.api.libs.json._

case class FeedbackBlockTransformer(qti: Node) extends InteractionTransformer {

  override def transform(node: Node): Seq[Node] = node
  override def interactionJs(qti: Node) = FeedbackBlockTransformer.interactionJs(qti)

}

object FeedbackBlockTransformer extends Transformer with XMLNamespaceClearer {

  val defaultCorrectness = true
  val outcomeIdentifier = """responses\.(.+?)\.(.*)""".r
  val outcomeSpecificRegex = "outcome.(.*)".r
  val DEFAULT_FEEDBACK = ""

  implicit class NodeWithFeedback(node: Node) {
    def isFeedbackFor(id: String) = (node \\ "@outcomeIdentifier").text match {
      case outcomeIdentifier(ident, _) => ident match {
        case `id` => true
        case _ => false
      }
      case _ => false
    }
  }

  def interactionJs(qti: Node) = (qti \\ "feedbackBlock").map(node => {
    def feedbackToJson(feedbackBlock:Node) = {
      val input = (feedbackBlock \ "@identifier").text
      def nonEmptyNode(c:Node) = !c.text.trim.isEmpty
      def formatNode(c:Node) = clearNamespace(c.child).mkString.trim
      val feedback = JsString(feedbackBlock.child.filter(nonEmptyNode).headOption.map(formatNode).getOrElse(DEFAULT_FEEDBACK))
      input match {
        case "" => Json.obj("input" -> "*", "feedback" -> feedback)
        case _ => Json.obj("input" -> input, "feedback" -> feedback)
      }
    }
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
        feedbackId -> Json.obj(
          "componentType" -> "corespring-feedback-block",
          "target" -> Json.obj("id" -> id),
          "weight" -> 0,
          "feedback" -> (outcomeSpecific match {
            case true => Json.obj(
              "outcome" -> ((node \ "@outcomeIdentifier").text match {
                case outcomeIdentifier(id, outcomeSpecificRegex(responseIdentifier)) => Json.obj(
                  responseIdentifier -> Json.obj(
                    "text" -> node.child.mkString,
                    "correct" -> ((node \ "@incorrectResponse").toString match {
                      case "true" => false
                      case "false" => true
                      case _ => (node \\ "div").find(n => Seq("feedback-block-correct", "feedback-block-incorrect")
                        .find(c => (n \ "@class").text.contains(c)).isDefined) match {
                        case Some(node) => (node \ "@class").text.contains("feedback-block-correct")
                        case _ => defaultCorrectness
                      }
                    })))
                case _ => throw new IllegalStateException("Node previously identified as outcome specific.")
              }))
            case false => Json.obj(
              "correct" -> JsArray(
                (qti \\ "feedbackBlock").filter(n => n.isFeedbackFor(id) && (n \ "@incorrectResponse").toString != "true").map(feedbackBlock => {
                   feedbackToJson(feedbackBlock)
                })),
              "incorrect" -> JsArray(
                (qti \\ "feedbackBlock").filter(n => n.isFeedbackFor(id) && (n \ "@incorrectResponse").toString == "true").map(feedbackBlock => {
                  feedbackToJson(feedbackBlock)
                })))
          }))

      }
      case _ =>
        throw new IllegalArgumentException(s"Malformed feedbackBlock outcomeIdentifier: ${(node \\ "@outcomeIdentifier").text}")
    }
  }).toMap

  /**
   * Takes a QTI document rooted at the provided node, removing <feedbackBlock/>s with duplicate outcomeIdentifier
   * attributes and replacing them with a single <corespring-feedback-block/> element.
   */
  override def transform(qti: Node): Node = {
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
            <corespring-feedback-block id={ feedbackId }></corespring-feedback-block>
          }
        }
      }
      case e: Elem => e.copy(child = e.nonEmptyChildren.map(recurse(_).headOption).flatten)
      case _ => node
    }

    recurse(qti).head
  }

}