package org.corespring.poc.integration.impl.transformers.qti.interactions

import scala.xml.transform.RewriteRule
import scala.xml.{Elem, Node}
import play.api.libs.json.{JsString, Json, JsObject}
import scala.collection.mutable
import scala.Predef._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

class FeedbackBlockTransformer(componentJson: mutable.Map[String, JsObject], qti: Node) extends RewriteRule {

  var feedbackNodes = mutable.Seq[Node]()

  private def feedback(n: Node, identifier: String) = {
    val feedbackBlocks = (n \\ "feedbackBlock")
      .filter(n => (n \ "@outcomeIdentifier").toString == s"responses.$identifier.value")
    feedbackNodes = feedbackNodes :+ feedbackBlocks.head
    Json.obj(
      "componentType" -> "corespring-feedback-block",
      "target" -> Json.obj("id" -> identifier),
      "feedback" -> Json.obj(
        "correct" -> JsObject(
          feedbackBlocks.filter(n => (n \ "@incorrectResponse").toString != "true").map(feedbackBlock => {
            (feedbackBlock \ "@identifier").text match {
              case "" => "*" -> JsString(feedbackBlock.child.text.trim)
              case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
            }
          })
        ),
        "incorrect" -> JsObject(
          feedbackBlocks.filter(n => (n \ "@incorrectResponse").toString == "true").map(feedbackBlock => {
            (feedbackBlock \ "@identifier").text match {
              case "" => "*" -> JsString(feedbackBlock.child.text.trim)
              case _ => (feedbackBlock \ "@identifier").text -> JsString(feedbackBlock.child.text.trim)
            }
          })
        )
      )
    )
  }

  (qti \\ "textEntryInteraction").map(t => (t \ "@responseIdentifier").text).foreach(id => {
    componentJson.put(s"${id}_feedback", feedback(qti, id))
  })

  val outcomeIdentifier = """responses\.(.+?)\..*""".r
  var renderedIds = mutable.Set[String]()
  var counter = 0

  override def transform(node: Node): Seq[Node] = {
    node match {
      case e: Elem if e.label == "feedbackBlock" => {
        (e \ "@outcomeIdentifier").text match {
          case outcomeIdentifier(id) if !feedbackNodes.contains(e) => Seq.empty
          case outcomeIdentifier(id) => <corespring-feedback-block id={s"${id}_feedback"}/>
          case _ => { println(e); Seq.empty }
        }
      }
      case _ => node
    }
  }

}
