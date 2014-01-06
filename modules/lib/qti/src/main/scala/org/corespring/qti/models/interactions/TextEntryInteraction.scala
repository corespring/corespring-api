package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses._
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.{ ResponseDeclaration, CorrectResponseEquation }
import xml.Node
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class TextEntryInteraction(responseIdentifier: String, expectedLength: Int, feedbackBlocks: Seq[FeedbackInline])
  extends Interaction with DefaultResponseConverter {

  def isScoreable = true

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = {
    def checkLineEquation: Option[CorrectResponseEquation] = responseDeclaration.flatMap(_.correctResponse.
      find(cr => cr.isInstanceOf[CorrectResponseEquation]).
      map[CorrectResponseEquation](cr => cr.asInstanceOf[CorrectResponseEquation]))
    response match {
      case StringResponse(_, responseValue, _) => responseDeclaration match {
        case Some(rd) => {
          def getOutcomeProperties(isCorrect: Boolean): Map[String, Boolean] = checkLineEquation match {
            case Some(cre) => if (isCorrect && cre.value != responseValue){
              if (!rd.exactMatch && rd.processInput(cre.value) == rd.processInput(responseValue)){
                Map()
              }else{
                //even though the response value may not match the expected value, the response may still be correct. lineEquationMatch is needed for backwards compatibility
                Map("equationMatch" -> true, "lineEquationMatch" -> true)
              }
            }else if (!isCorrect) Map("incorrectEquation" -> true)
            else Map()
            case None => Map()
          }
          rd.mapping match {
            case Some(mapping) =>
              val isCorrect: Boolean = rd.isCorrect(responseValue) == Correctness.Correct
              Some(ResponseOutcome(mapping.mappedValue(response.value), isCorrect, outcomeProperties = getOutcomeProperties(isCorrect)))
            case None => {
              val isCorrect: Boolean = rd.isCorrect(responseValue) == Correctness.Correct
              Some(ResponseOutcome(if (isCorrect) 1 else 0, isCorrect, outcomeProperties = getOutcomeProperties(isCorrect)))
            }
          }
        }
        case None => None
      }
      //in this case, multiple responses are received, but this interaction only allows for one correct answer. choose the correct answer with the highest value
      case ArrayResponse(_, responseValues, _) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => {
            var max: Float = Float.MinValue;
            for (responseValue <- responseValues) {
              val mappedValue = mapping.mappedValue(responseValue)
              if (max < mappedValue) max = mappedValue
            }
            Some(ResponseOutcome(max, true))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ResponseOutcome(1, true))
          } else Some(ResponseOutcome(0, false))
        }
        case None => None
      }
      case _ => {
        logger.error("received a response that was not a string response in TextEntryInteraction.getOutcome")
        None
      }
    }
  }
}

object TextEntryInteraction extends InteractionCompanion[TextEntryInteraction] {

  def tagName = "textEntryInteraction"

  def apply(node: Node, itemBody: Option[Node]): TextEntryInteraction = {
    val responseIdentifier = Interaction.responseIdentifier(node)
    TextEntryInteraction(
      responseIdentifier = responseIdentifier,
      expectedLength = expectedLength(node),
      feedbackBlocks = itemBody match {
        case Some(node) => {
          val fb = feedbackBlocks(node)
          fb.filter(_.outcomeIdentifier == responseIdentifier)
        }
        case None => throw new RuntimeException("this interaction requires a reference to the outer qti model");
      })
  }
  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "textEntryInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => TextEntryInteraction(node, Some(itemBody)))
    }
  }
  private def feedbackBlocks(itemBody: Node): Seq[FeedbackInline] = {
    (itemBody \\ "feedbackBlock").map(node => FeedbackInline(node, None))
  }
  private def expectedLength(n: Node): Int = (n \ "@expectedLength").text.toInt

}
