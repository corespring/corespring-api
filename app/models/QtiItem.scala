package models.bleezmo

import scala.xml._

case class QtiItem(responseDeclarations:Seq[ResponseDeclaration],itemBody:ItemBody){
  var _defaultCorrect = "That is correct!"
  var _defaultIncorrect = "That is incorrect"
  def defaultCorrect = _defaultCorrect
  def defaultIncorrect = _defaultIncorrect
}
object QtiItem{
  def apply(node:Node):QtiItem = QtiItem(
    (node \ "responseDeclaration").map(ResponseDeclaration(_)),
    ItemBody((node \ "itemBody").head)
  )
}
case class ResponseDeclaration(identifier:String, cardinality:String, correctResponse:Option[CorrectResponse])
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
case class FeedbackInline(csFeedbackId:String, content: String, var defaultFeedback:Boolean = false)
object FeedbackInline{
  def apply(node:Node):FeedbackInline = {
    val feedbackInline = FeedbackInline((node \ "@csFeedbackId").text, node.child.text)
    (node \ "@defaultFeedback") match {
      case Text(defaultFeedback) => feedbackInline.defaultFeedback = if(defaultFeedback == "true") true else false
      case _ =>
    }
    feedbackInline
  }

}

