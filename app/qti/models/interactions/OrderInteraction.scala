package qti.models.interactions

import choices.SimpleChoice
import xml.Node
import qti.models.ResponseDeclaration
import qti.models.QtiItem.Correctness
import controllers.Log
import models.itemSession._

case class OrderInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices {

  def isScoreable = true


  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    response match {
      case ArrayItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => {
            var count:Int = 0;
            var sum:Float = 0;
            var correctCount:Int = 0;
            for (value <- responseValue){
              if (rd.isValueCorrect(value,Some(count))){
                sum += mapping.mappedValue(value)
                correctCount += 1;
              }
              count += 1;
            }
            Some(ItemResponseOutcome(sum,rd.isCorrect(responseValue) == Correctness.Correct))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct){
            Some(ItemResponseOutcome(1,true))
          } else {
            Some(ItemResponseOutcome(0,false))
          }
        }
        case None => None
      }
      case _ => {
        Log.e("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object OrderInteraction extends InteractionCompanion[OrderInteraction]{
  def tagName = "orderInteraction"
  def apply(node: Node, itemBody:Option[Node]): OrderInteraction = OrderInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_, (node \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "orderInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => OrderInteraction(node,Some(itemBody)))
    }
  }
}
