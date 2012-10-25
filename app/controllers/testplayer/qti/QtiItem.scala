package controllers.testplayer.qti

import scala.xml._
import play.api.libs.json.{JsString, JsObject, JsValue, Writes}

case class QtiItem(responseDeclarations:Seq[ResponseDeclaration],itemBody:ItemBody, modalFeedbacks:Seq[FeedbackInline]){
  var defaultCorrect = "That is correct!"
  var defaultIncorrect = "That is incorrect"
}
object QtiItem{
  def apply(node:Node):QtiItem = {
    val qtiItem = QtiItem((node \ "responseDeclaration").map(ResponseDeclaration(_)), ItemBody((node \ "itemBody").head), (node \ "modalFeedbacks").map(FeedbackInline(_,None)))
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
  def getAllFeedback(qtiItem:QtiItem):Seq[FeedbackInline] = {
    qtiItem.modalFeedbacks ++ qtiItem.itemBody.feedbackBlocks ++ qtiItem.itemBody.interactions.map(_ match {
      case ChoiceInteraction(_,choices) => choices.map(sc => sc.feedbackInline)
      case OrderInteraction(_,choices) => choices.map(sc => sc.feedbackInline)
      case _ => Seq()
    }).flatten.flatten
  }
}
case class ResponseDeclaration(identifier:String, cardinality:String, correctResponse:Option[CorrectResponse],mapping:Option[Mapping]){
  def isCorrect(responseValue:String):Boolean = correctResponse match {
    case Some(cr) => cr.isCorrect(responseValue)
    case None => throw new RuntimeException("no correct response to check")
  }
  def mappedValue(mapKey:String):String = mapping match {
    case Some(m) => m.mappedValue(mapKey)
    case None => throw new RuntimeException("no mapping for this response declaration")
  }
}
object ResponseDeclaration{
  def apply(node:Node):ResponseDeclaration = ResponseDeclaration(
    (node \ "@identifier").text,
    (node \ "@cardinality").text,
    (node \ "correctResponse").headOption.map(CorrectResponse(_,(node \ "@cardinality").text)),
    (node \ "mapping").headOption.map(Mapping(_))
  )
}
trait CorrectResponse{
  def isCorrect(responseValue:String):Boolean
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
  def isCorrect(responseValue:String):Boolean = responseValue == value
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
  def isCorrect(responseValue:String) = value.find(_ == responseValue).isDefined
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
case class Mapping(mapEntries:Map[String,String],defaultValue:Option[String]) {
  def mappedValue(mapKey:String):String = {
    mapEntries.get(mapKey) match {
      case Some(mappedValue) => mappedValue
      case None => defaultValue match {
        case Some(dv) => dv
        case None => throw new RuntimeException("no value found for given key")
      }
    }
  }
}
object Mapping{
  def apply(node:Node):Mapping = {
    val defaultValue = (node \ "@defaultValue").text match {
      case x:String if x.nonEmpty => Some(x)
      case _ => None
    }
    val mapEntries = (node \ "mapEntry").foldRight[Map[String,String]](Map())((node,acc) =>
      acc + ((node \ "@mapKey").text -> (node \ "@mappedValue").text)
    )
    Mapping(mapEntries, defaultValue)
  }
}
case class ItemBody(interactions:Seq[Interaction],feedbackBlocks:Seq[FeedbackInline])
object ItemBody{
  def apply(node:Node):ItemBody = {
    var interactions:Seq[Interaction] = Seq()
    var feedbackBlocks:Seq[FeedbackInline] = Seq()

    //Inline choice interactions can be nested within html elements so use \\ instead
    (node \\ "inlineChoiceInteraction").foreach( (n:Node) => {
      interactions = interactions :+ InlineChoiceInteraction(n)
    })

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

case class InlineChoiceInteraction(responseIdentifier:String, choices:Seq[InlineChoice]) extends Interaction

object InlineChoiceInteraction{
  def apply(node:Node) : InlineChoiceInteraction = InlineChoiceInteraction(

  (node\"@responseIdentifier").text,
  (node \ "inlineChoice").map(InlineChoice(_,(node \ "@responseIdentifier").text))
  )
}

case class InlineChoice(identifier:String,  responseIdentifier:String, feedbackInline:Option[FeedbackInline])

object InlineChoice{
  def apply(node:Node, responseIdentifier:String): InlineChoice = InlineChoice(
   (node\"@identifier").text,
   responseIdentifier,
   (node\"feedbackInline").headOption.map(FeedbackInline(_, Some(responseIdentifier)))
 )
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
case class FeedbackInline(csFeedbackId:String, outcomeIdentifier:String, identifier:String, content: String, var defaultFeedback:Boolean = false, var incorrectResponse:Boolean = false){
  def defaultContent(qtiItem:QtiItem):String =
    qtiItem.responseDeclarations.find(_.identifier == outcomeIdentifier) match {
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

    def isNullOrEmpty( s : String ) : Boolean = (s == null || s.length == 0)

    if ( node.label == "feedbackInline")
      require( !isNullOrEmpty((node \ "@identifier").text ), node )

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
  implicit object FeedbackInlineWrites extends Writes[FeedbackInline]{
    override def writes(fi:FeedbackInline):JsValue = {
      JsObject(Seq(
        "csFeedbackId" -> JsString(fi.csFeedbackId),
        "responseIdentifier" -> JsString(fi.outcomeIdentifier),
        "identifier" -> JsString(fi.identifier),
        "body" -> JsString(fi.content)
      ))
    }
  }
}

