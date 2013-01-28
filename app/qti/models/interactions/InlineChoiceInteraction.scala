package qti.models.interactions

import choices.{InlineChoice, Choice}
import xml.{Elem, Node}
import qti.models.ResponseDeclaration
import models.{StringItemResponse, ItemResponseOutcome, ItemResponse}
import qti.models.QtiItem.Correctness
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class InlineChoiceInteraction(responseIdentifier: String, choices: Seq[InlineChoice]) extends InteractionWithChoices {
  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(mapping.mappedValue(response.value)))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1))
          } else Some(ItemResponseOutcome(0))
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

object InlineChoiceInteraction extends InteractionCompanion[InlineChoiceInteraction]{
  def interactionLabel: String = "inlineChoiceInteraction"
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

