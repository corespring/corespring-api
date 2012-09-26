package models

import play.api.libs.json._
import play.api.libs.json.JsString
import scala.xml._
import models.bleezmo.{OrderInteraction, ChoiceInteraction, CorrectResponseMultiple, CorrectResponseSingle}


/**
 * data sent back after a session update (representing a completed item). example:
 *
 * {
 *         sessionData: {
 *             feedbackContents: {
 *                 [csFeedbackId]: "[contents of feedback element]",
 *                 [csFeedbackId]: "[contents of feedback element]"
 *             }
 *             correctResponse: {
 *                 irishPresident: "higgins",
 *                 rainbowColors: ['blue','violet', 'red']
 *             }
 *         }
 *   }
 */
case class SessionData(qtiItem: bleezmo.QtiItem)
object SessionData{
  implicit object SessionDataWrites extends Writes[SessionData]{
    def writes(sd: SessionData) = {
      var correctResponses:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.responseDeclarations.foreach(rd => {
        rd.correctResponse.foreach( _ match {
          case crs:CorrectResponseSingle => correctResponses = correctResponses :+ (rd.identifier -> JsString(crs.value))
          case crm:CorrectResponseMultiple => correctResponses = correctResponses :+ (rd.identifier -> JsArray(crm.value.map(JsString(_))))
          case _ => throw new RuntimeException("unexpected correct response type")
        })
      })
      var feedbackContents:Seq[(String,JsValue)] = Seq()
      sd.qtiItem.itemBody.interactions.foreach(interaction => {
        interaction match {
          case ci:ChoiceInteraction => ci.choices.map(choice => choice.feedbackInline.foreach(fi =>
            feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(fi.content))
          ))
          case oi:OrderInteraction => oi.choices.map(choice => choice.feedbackInline.foreach(fi =>
            feedbackContents = feedbackContents :+ (fi.csFeedbackId -> JsString(fi.content))
          ))
        }
      })
      JsObject(Seq(
        "feedbackContents" -> JsObject(feedbackContents),
        "correctResponses" -> JsObject(correctResponses)
      ))
    }
  }

}
package bleezmo {
/*******************move this to qti item somehow*******************/
case class QtiItem(responseDeclarations:Seq[ResponseDeclaration],itemBody:ItemBody)
object QtiItem{
  def apply(node:Node):QtiItem = QtiItem(
    (node \ "responseDeclaration").map(ResponseDeclaration(_)),
    ItemBody((node \ "itemBody").head)
  )
}
case class ResponseDeclaration(identifier:String, cardinality:String,correctResponse:Option[CorrectResponse])
object ResponseDeclaration{
  def apply(node:Node):ResponseDeclaration = ResponseDeclaration(
    (node \ "@identifier").text,
    (node \ "@cardinality").text,
    (node \ "correctResponse").headOption.map(CorrectResponse(_,(node \ "@cardinality").text))
  )
}
trait CorrectResponse
object CorrectResponse{
  def apply(node:Node,cardinality:String):CorrectResponse = {
    cardinality match {
      case "single" => CorrectResponseSingle(node)
      case "multiple" => CorrectResponseMultiple(node)
      case "ordered" => CorrectResponseMultiple(node)
      case _ => throw new RuntimeException("unknown cardinality: "+cardinality+". cannot generate CorrectResponse")
    }
  }
}
case class CorrectResponseSingle(value: String) extends CorrectResponse
object CorrectResponseSingle{
  def apply(node:Node):CorrectResponseSingle = CorrectResponseSingle(
    (node \ "value").text
  )
}
case class CorrectResponseMultiple(value: Seq[String]) extends CorrectResponse
object CorrectResponseMultiple{
  def apply(node:Node):CorrectResponseMultiple = CorrectResponseMultiple(
    (node \ "value").map(_.text)
  )
}
case class ItemBody(interactions:Seq[Interaction])
object ItemBody{
  def apply(node:Node):ItemBody = {
    var interactions:Seq[Interaction] = Seq()
    node.child.foreach(inner => {
      inner.label match {
        case "choiceInteraction" => interactions = interactions :+ ChoiceInteraction(inner)
        case "orderInteraction" => interactions = interactions :+ OrderInteraction(inner)
        case _ =>
      }
    })
    ItemBody(interactions)
  }
}
trait Interaction{
  val identifier:String
}
case class ChoiceInteraction(identifier:String, choices:Seq[SimpleChoice]) extends Interaction
object ChoiceInteraction{
  def apply(node:Node):ChoiceInteraction = ChoiceInteraction(
    (node \ "@identifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_))
  )
}
case class OrderInteraction(identifier:String, choices:Seq[SimpleChoice]) extends Interaction
object OrderInteraction{
  def apply(node:Node):OrderInteraction = OrderInteraction(
    (node \ "@identifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_))
  )
}
case class SimpleChoice(identifier: String, feedbackInline: Option[FeedbackInline])
object SimpleChoice{
  def apply(node:Node):SimpleChoice = SimpleChoice(
    (node \ "@identifier").text,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_))
  )
}
case class FeedbackInline(csFeedbackId:String, content: String)
object FeedbackInline{
  def apply(node:Node):FeedbackInline = FeedbackInline(
    (node \ "@csFeedbackId").text,
    node.child.text
  )
}
}


