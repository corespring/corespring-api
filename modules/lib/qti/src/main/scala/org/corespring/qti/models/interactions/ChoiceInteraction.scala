package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses._
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.ResponseDeclaration
import org.corespring.qti.models.interactions.choices.{ SimpleChoice, Choice }
import scala.Some
import xml._
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class ChoiceInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices with DefaultResponseConverter {

  def isScoreable = true

  def getChoice(identifier: String): Option[Choice] = choices.find(_.identifier == identifier)

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
      case ArrayResponse(_, responseValues, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ResponseOutcome(
            responseValues.foldRight[Float](0)((responseValue, sum) => sum + mapping.mappedValue(responseValue)),
            rd.isCorrect(responseValues) == Correctness.Correct))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ResponseOutcome(1, true))
          } else Some(ResponseOutcome(0, false))
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

object ChoiceInteraction extends InteractionCompanion[ChoiceInteraction] {

  def tagName = "choiceInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): ChoiceInteraction = ChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "simpleChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text)))

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => ChoiceInteraction(node, Some(itemBody)))
    }
  }

  override def preProcessXml(interactionXml: Elem): NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(ChoiceInteraction(interactionXml, None)).transform(interactionXml)
  }

}
