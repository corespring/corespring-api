package models.bleezmo

import scala.xml._

case class QtiItem(responseDeclarations:Seq[ResponseDeclaration],itemBody:ItemBody){
  var defaultCorrect = "That is correct!"
  var defaultIncorrect = "That is incorrect"
}
object QtiItem{
  def apply(node:Node):QtiItem = {
    val qtiItem = QtiItem((node \ "responseDeclaration").map(ResponseDeclaration(_)), ItemBody((node \ "itemBody").head))
    (node \ "correctResponseFeedback").headOption match {
      case Some(correctResponseFeedback) => qtiItem.defaultCorrect = correctResponseFeedback.child.text
      case None =>
    }
    (node \ "incorrectResponseFeedback").headOption match {
      case Some(incorrectResponseFeedback) => qtiItem.defaultIncorrect = incorrectResponseFeedback.child.text
      case None =>
    }
    qtiItem
  }

}
case class ResponseDeclaration(identifier:String, cardinality:String, correctResponse:Option[CorrectResponse]){
  def isCorrect(responseIdentifier:String):Boolean = correctResponse match {
    case Some(cr) => cr.isCorrect(responseIdentifier)
    case None => throw new RuntimeException("no correct response to check")
  }
}
object ResponseDeclaration{
  def apply(node:Node):ResponseDeclaration = ResponseDeclaration(
    (node \ "@identifier").text,
    (node \ "@cardinality").text,
    (node \ "correctResponse").headOption.map(CorrectResponse(_,(node \ "@cardinality").text))
  )
}
trait CorrectResponse{
  def isCorrect(identifier:String):Boolean
}
object CorrectResponse{
  def apply(node:Node,cardinality:String):CorrectResponse = {
    cardinality match {
      case "single" => {
        if( (node\"value").length > 1 ){
         CorrectResponseMultiple(node)
        }else {
          CorrectResponseSingle(node)
        }
      }
      case "multiple" => CorrectResponseMultiple(node)
      case "ordered" => CorrectResponseOrdered(node)
      case _ => throw new RuntimeException("unknown cardinality: "+cardinality+". cannot generate CorrectResponse")
    }
  }
}
case class CorrectResponseSingle(value: String) extends CorrectResponse{
  def isCorrect(identifier:String):Boolean = identifier == value
}
object CorrectResponseSingle{
  def apply(node:Node):CorrectResponseSingle = {

    if ( (node \ "value" ).size != 1 ){
      throw new RuntimeException("Cardinality is set to single but there is not one <value> declared: " + (node\"value").toString)
    }
    else {
      CorrectResponseSingle( (node \ "value").text )
    }
  }
}

case class CorrectResponseMultiple(value: Seq[String]) extends CorrectResponse{
  def isCorrect(identifier:String) = value.find(_ == identifier).isDefined
}
object CorrectResponseMultiple{
  def apply(node:Node):CorrectResponseMultiple = CorrectResponseMultiple(
    (node \ "value").map(_.text)
  )
}
case class CorrectResponseOrdered(value: Seq[String]) extends CorrectResponse{
  def isCorrect(identifier:String) = value.find(_ == identifier).isDefined
}
object CorrectResponseOrdered{
  def apply(node:Node):CorrectResponseOrdered = CorrectResponseOrdered(
    (node \ "value").map(_.text)
  )
}
case class ItemBody(interactions:Seq[Interaction],feedbackBlocks:Seq[FeedbackInline])
object ItemBody{
  def apply(node:Node):ItemBody = {
    var interactions:Seq[Interaction] = Seq()
    var feedbackBlocks:Seq[FeedbackInline] = Seq()
    node.child.foreach(inner => {
      inner.label match {
        case "choiceInteraction" => interactions = interactions :+ ChoiceInteraction(inner)
        case "orderInteraction" => interactions = interactions :+ OrderInteraction(inner)
        case "feedbackBlock" => feedbackBlocks = feedbackBlocks :+ FeedbackInline(inner,None)

        case _ =>
      }
    })
    ItemBody(interactions,feedbackBlocks)
  }
}
trait Interaction{
  val responseIdentifier:String
}
case class ChoiceInteraction(responseIdentifier:String, choices:Seq[SimpleChoice]) extends Interaction
object ChoiceInteraction{
  def apply(node:Node):ChoiceInteraction = ChoiceInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_,(node \ "@responseIdentifier").text))
  )
}
case class OrderInteraction(responseIdentifier:String, choices:Seq[SimpleChoice]) extends Interaction
object OrderInteraction{
  def apply(node:Node):OrderInteraction = OrderInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_,(node \ "@responseIdentifier").text))
  )
}
case class SimpleChoice(identifier: String, responseIdentifier:String, feedbackInline: Option[FeedbackInline])
object SimpleChoice{
  def apply(node:Node,responseIdentifier:String):SimpleChoice = SimpleChoice(
    (node \ "@identifier").text,
    responseIdentifier,
    (node \ "feedbackInline").headOption.map(FeedbackInline(_,Some(responseIdentifier)))
  )
}
case class ModalFeedback(csFeedbackId:String, responseIdentifier:String)
case class FeedbackInline(csFeedbackId:String, responseIdentifier:String, identifier:String, content: String, var defaultFeedback:Boolean = false, var incorrectResponse:Boolean = false){
  def defaultContent(qtiItem:QtiItem):String =
    qtiItem.responseDeclarations.find(_.identifier == responseIdentifier) match {
    case Some(rd) =>
      if(rd.isCorrect(identifier)) qtiItem.defaultCorrect else qtiItem.defaultIncorrect
    case None => ""
  }
}
object FeedbackInline{
  /**
   * if this feedbackInline is within a interaction, responseIdentifier should be pased in
   * otherwise, if the feedbackInline is within itemBody, then the feedbackInline must have an outcomeIdentifier (equivalent to responseIdentifier) which must be parsed
   * @param node
   * @param responseIdentifier
   * @return
   */
  def apply(node:Node,responseIdentifier:Option[String]):FeedbackInline = {
    val childBody = new StringBuilder
    node.child.map(
      node => childBody.append(node.toString()))
    def contents: String = childBody.toString()
    val feedbackInline = responseIdentifier match {
      case Some(ri) => FeedbackInline((node \ "@csFeedbackId").text,
        ri,
        (node \ "@identifier").text, contents,
        (node \ "@defaultFeedback").text == "true",
        (node \ "@incorrectResponse").text == "true")
      case None => FeedbackInline((node \ "@csFeedbackId").text,
        (node \ "@outcomeIdentifier").text.split('.')(1),
        (node \ "@identifier").text, contents,
        (node \ "@defaultFeedback").text == "true",
        (node \ "@incorrectResponse").text == "true")
    }
    feedbackInline
  }

}

