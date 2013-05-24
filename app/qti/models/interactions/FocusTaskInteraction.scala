package qti.models.interactions

import choices.{Choice, SimpleChoice}
import models.itemSession._
import qti.models.{QtiItem, CorrectResponseMultiple, ResponseDeclaration}
import scala.Some
import xml._

case class FocusTaskInteraction(responseIdentifier: String, choices: Seq[SimpleChoice], checkIfCorrect: Boolean, minSelections: Int, maxSelections: Int) extends InteractionWithChoices {

  def isScoreable = true

  override def validate(qtiItem: QtiItem) = {
    val hasResponse = !(qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty)
    if (!checkIfCorrect || (checkIfCorrect && hasResponse))
      (true, "Ok")
    else
      (false, "Missing response declartaion for " + responseIdentifier)
  }

  def getChoice(identifier: String): Option[Choice] = choices.find(_.identifier == identifier)

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    var outcomeProperties: Map[String, Boolean] = Map()
    var score: Float = 0
    var isResponseCorrect: Boolean = true
    var isResponseIncorrect: Boolean = false
    response match {
      case ArrayItemResponse(_, responseValues, _) =>
        val isNumberOfSelectionCorrect = responseValues.size >= minSelections && responseValues.size <= maxSelections
        isResponseCorrect = isNumberOfSelectionCorrect

        responseDeclaration match {
          case Some(rd) =>
            if (checkIfCorrect) {
              rd.correctResponse match {
                case Some(cr: CorrectResponseMultiple) =>
                  isResponseCorrect = cr.isPartOfCorrect(response.value)
                  isResponseIncorrect = !isResponseCorrect
                case _ =>
              }
            }

            score = rd.mapping match {
              case Some(mapping) =>
                responseValues.foldRight[Float](0)((responseValue, sum) => sum + mapping.mappedValue(responseValue))
              case _ =>
                if (isResponseCorrect && isNumberOfSelectionCorrect) 1 else 0
            }
          case None =>
            score = if (isResponseCorrect && isNumberOfSelectionCorrect) 1 else 0
        }

        outcomeProperties = outcomeProperties + ("responsesNumberCorrect" -> isNumberOfSelectionCorrect)
        outcomeProperties = outcomeProperties + ("responsesIncorrect" -> (isResponseIncorrect && isNumberOfSelectionCorrect))
        outcomeProperties = outcomeProperties + ("responsesCorrect" -> (isResponseCorrect && isNumberOfSelectionCorrect))
        outcomeProperties = outcomeProperties + ("responsesExceedMax" -> (responseValues.size > maxSelections))
        outcomeProperties = outcomeProperties + ("responsesBelowMin" -> (responseValues.size < minSelections))

      case _ =>
        Logger.error("received a response that was not an array response in FocusTaskInteraction.getOutcome")
    }


    Some(ItemResponseOutcome(score, isResponseCorrect, None, outcomeProperties))
  }
}

object FocusTaskInteraction extends InteractionCompanion[FocusTaskInteraction] {

  def tagName: String = "focusTaskInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): FocusTaskInteraction = {

    FocusTaskInteraction(
      (interaction \ "@responseIdentifier").text,
      (interaction \ "focusChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text)),
      (interaction \ "@checkIfCorrect").text == "yes",
      (interaction \ "@minSelections").text.toInt,
      (interaction \ "@maxSelections").text.toInt
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "focusTaskInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => FocusTaskInteraction(node, Some(itemBody)))
    }
  }

  override def preProcessXml(interactionXml: Elem): NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(FocusTaskInteraction(interactionXml, None)).transform(interactionXml)
  }

}
