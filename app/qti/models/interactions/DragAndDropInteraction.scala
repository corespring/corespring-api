package qti.models.interactions

import choices.SimpleChoice
import models.itemSession._
import qti.models.QtiItem.Correctness
import qti.models.ResponseDeclaration
import xml.Node

case class DragAndDropInteraction(responseIdentifier: String, choices: Seq[SimpleChoice], orderMatters:Boolean = false) extends InteractionWithChoices {

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
        Logger.error("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object DragAndDropInteraction extends InteractionCompanion[DragAndDropInteraction]{
  def tagName = "dragAndDropInteraction"
  def apply(node: Node, itemBody:Option[Node]): DragAndDropInteraction = DragAndDropInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "draggableAnswer").map(SimpleChoice(_, (node \ "@responseIdentifier").text)),
    (node \ "@orderMatters").text == "true"
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "dragAndDropInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => DragAndDropInteraction(node,Some(itemBody)))
    }
  }
}
