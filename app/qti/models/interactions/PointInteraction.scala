package qti.models.interactions

import qti.models.ResponseDeclaration
import models.itemSession.{StringItemResponse, ItemResponseOutcome, ItemResponse}
import xml.Node
import qti.models.QtiItem.Correctness

case class PointInteraction(responseIdentifier: String) extends Interaction {
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => {
          val isCorrect = rd.isCorrect(responseValue) == Correctness.Correct
          rd.mapping match {
            case Some(mapping) => Some(
              ItemResponseOutcome(mapping.mappedValue(response.value),
                isCorrect, outcomeProperties = if (isCorrect) Map("correct" -> true) else Map("incorrect" -> true)
              )
            )
            case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
              Some(ItemResponseOutcome(1, true, outcomeProperties = Map("correct" -> true)))
            } else Some(ItemResponseOutcome(0,false,outcomeProperties = Map("incorrect" -> true)))
          }
        }
        case None => None
      }
    }
  }

  /** Can this Interaction be automatically scored from the users response
    * Eg: multichoice can - but free written text can't be
    * @return
    */
  def isScoreable: Boolean = false
}

object PointInteraction extends InteractionCompanion[PointInteraction]{
  def tagName: String = "pointInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): PointInteraction = {
    PointInteraction(
      (interaction \ "@responseIdentifier").text
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => PointInteraction(node, Some(itemBody)))
    }
  }
}