package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses._
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.ResponseDeclaration
import org.corespring.qti.models.interactions.choices.InlineChoice
import xml.Node
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class InlineChoiceInteraction(responseIdentifier: String, choices: Seq[InlineChoice])
  extends InteractionWithChoices with DefaultResponseConverter {

  def isScoreable = true

  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    response match {
      case StringResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ResponseOutcome(mapping.mappedValue(response.value), rd.isCorrect(responseValue) == Correctness.Correct))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ResponseOutcome(1, true))
          } else Some(ResponseOutcome(0, false))
        }
        case None => None
      }
      case _ => {
        logger.error("received a response that was not a string response in InlineChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object InlineChoiceInteraction extends InteractionCompanion[InlineChoiceInteraction] {

  def tagName: String = "inlineChoiceInteraction"
  def apply(interaction: Node, itemBody: Option[Node] = None): InlineChoiceInteraction = InlineChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "inlineChoice").map(InlineChoice(_, (interaction \ "@responseIdentifier").text)))
  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "inlineChoiceInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => InlineChoiceInteraction(node, Some(itemBody)))
    }
  }
}

