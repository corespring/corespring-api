package qti.models.interactions

import qti.models.ResponseDeclaration
import models.itemSession.{StringItemResponse, ItemResponseOutcome, ItemResponse}
import xml.Node
import qti.models.QtiItem.Correctness

case class LineInteraction(responseIdentifier: String) extends Interaction{

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(mapping.mappedValue(response.value), rd.isCorrect(responseValue) == Correctness.Correct))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1,true))
          } else Some(ItemResponseOutcome(0,false))
        }
        case None => None
      }
      case _ => {
        Logger.error("received a response that was not a string response in LineInteraction.getOutcome")
        None
      }
    }
  }

  /** Can this Interaction be automatically scored from the users response
    * Eg: multichoice can - but free written text can't be
    * @return
    */
  def isScoreable: Boolean = false
}
object LineInteraction extends InteractionCompanion[LineInteraction]{
  def tagName: String = "lineInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): LineInteraction = {
    LineInteraction(
      (interaction \ "@responseIdentifier").text
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => LineInteraction(node, Some(itemBody)))
    }
  }
}
