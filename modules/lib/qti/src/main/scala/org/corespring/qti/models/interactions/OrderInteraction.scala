package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses._
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.ResponseDeclaration
import org.corespring.qti.models.interactions.choices.SimpleChoice
import xml.Node
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class OrderInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices with DefaultResponseConverter {

  def isScoreable = true

  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    response match {
      case ArrayResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => {
            var count: Int = 0
            var sum: Float = 0
            var correctCount: Int = 0
            for (value <- responseValue) {
              if (rd.isValueCorrect(value, Some(count))) {
                sum += mapping.mappedValue(value)
                correctCount += 1
              }
              count += 1
            }
            Some(ResponseOutcome(sum, rd.isCorrect(responseValue) == Correctness.Correct))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ResponseOutcome(1, true))
          } else {
            Some(ResponseOutcome(0, false))
          }
        }
        case None => None
      }
      case _ => {
        logger.error("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object OrderInteraction extends InteractionCompanion[OrderInteraction] {
  def tagName = "orderInteraction"
  def apply(node: Node, itemBody: Option[Node]): OrderInteraction = OrderInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_, (node \ "@responseIdentifier").text)))
  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "orderInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => OrderInteraction(node, Some(itemBody)))
    }
  }
}
