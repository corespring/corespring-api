package models

import play.api.libs.json._
import play.api.libs.json.JsString
import scala.xml._


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
 * @param feedbackContents map of csFeedbackId's to outcome value's
 * @param correctResponses array of CorrectResponse objects, representing the mapping of response identifier's to correct response
 */
case class SessionData()
object SessionData{
  val feedbackContents = "feedbackContents"
  val correctResponses = "correctResponses"
  implicit object SessionDataWrites extends Writes[SessionData]{
    def writes(sd: SessionData) = {
      JsObject(Seq(
      ))
    }
  }

}
package bleezmo {
/*******************move this to qti item somehow*******************/
case class QtiItem(responseDeclarations:Seq[ResponseDeclaration],itemBody:ItemBody)
case class ResponseDeclaration(identifier:String, cardinality:String,correctResponse:CorrectResponse)
object ResponseDeclaration{
  def apply(node:Node):ResponseDeclaration = ResponseDeclaration(
    (node \ "@identifier").text,
    (node \ "@cardinality").text,
    CorrectResponse((node \ "correctResponse").head,(node \ "@cardinality").text)
  )
}
trait CorrectResponse
object CorrectResponse{
  def apply(node:Node,cardinality:String):CorrectResponse = {
    cardinality match {
      case "single" => CorrectResponseSingle(node)
      case "multiple" => CorrectResponseMultiple(node)
      case _ => throw new RuntimeException("unknown cardinality. cannot generate CorrectResponse")
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
    node.foreach(inner => if (inner.label contains "Interaction") {
      interactions = interactions :+ Interaction(inner)
    })
    ItemBody(interactions)
  }
}
trait Interaction{
  val identifier:String
}
object Interaction{
  def apply(node:Node):Interaction = {
    node.label match {
      case "choiceInteraction" => ChoiceInteraction(node)
      case _ => throw new RuntimeException("unknown interaction")
    }
  }
}
case class ChoiceInteraction(identifier:String, choices:Seq[SimpleChoice]) extends Interaction
object ChoiceInteraction{
  def apply(node:Node):ChoiceInteraction = ChoiceInteraction(
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
///*********************************************************************/
//case class CorrectResponses(var correctResponses:Seq[CorrectResponse] = Seq())
//object CorrectResponses{
//  implicit object CorrectResponsesWrites extends Writes[CorrectResponses]{
//    def writes(crs: CorrectResponses) = {
//      var crseq:Seq[(String,JsValue)] = Seq()
//      crs.correctResponses.foreach(cr => {
//        cr match {
//          case single:CorrectResponseSingle => crseq = crseq :+ (single.identifier -> JsString(single.value))
//          case multiple:CorrectResponseMultiple => crseq = crseq :+ (multiple.identifier -> JsArray(multiple.value.map(JsString(_))))
//        }
//      })
//      JsObject(crseq)
//    }
//  }
//}
}


