package qti.models.interactions

import choices.InlineChoice
import controllers.Log
import models.itemSession._
import qti.models.QtiItem.Correctness
import qti.models.ResponseDeclaration
import xml.Node

case class InlineChoiceInteraction(responseIdentifier: String, choices: Seq[InlineChoice]) extends InteractionWithChoices {

  def isScoreable = true


  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(mapping.mappedValue(response.value),rd.isCorrect(responseValue) == Correctness.Correct))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1,true))
          } else Some(ItemResponseOutcome(0,false))
        }
        case None => None
      }
      case _ => {
        Log.e("received a response that was not a string response in InlineChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object InlineChoiceInteraction extends InteractionCompanion[InlineChoiceInteraction]{
  def tagName: String = "inlineChoiceInteraction"
  def apply(interaction: Node, itemBody:Option[Node]): InlineChoiceInteraction = InlineChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "inlineChoice").map(InlineChoice(_, (interaction \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "inlineChoiceInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => InlineChoiceInteraction(node,Some(itemBody)))
    }
  }
}

