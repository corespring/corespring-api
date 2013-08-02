package qti.models.interactions

import org.corespring.platform.core.models.itemSession._
import qti.models.QtiItem.Correctness
import qti.models.{CorrectResponseLineEquation, ResponseDeclaration}
import xml.Node
import java.util.regex.Pattern
import util.Random
import javax.script.ScriptEngineManager

case class TextEntryInteraction(responseIdentifier: String, expectedLength: Int, feedbackBlocks: Seq[FeedbackInline]) extends Interaction {

  def isScoreable = true

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    def checkLineEquation:Option[CorrectResponseLineEquation] = responseDeclaration.flatMap(_.correctResponse.
        find(cr => cr.isInstanceOf[CorrectResponseLineEquation]).
        map[CorrectResponseLineEquation](cr => cr.asInstanceOf[CorrectResponseLineEquation])
    )
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => {
          def getOutcomeProperties(isCorrect:Boolean):Map[String,Boolean] = checkLineEquation match {
            case Some(cre) => if (isCorrect && cre.value != responseValue) Map("lineEquationMatch" -> true)  //even though the response value may not match the expected value, the response may still be correct
              else if(!isCorrect) Map("incorrectEquation" ->  true)
              else Map()
            case None => Map()
          }
          rd.mapping match {
            case Some(mapping) =>
              val isCorrect:Boolean = rd.isCorrect(responseValue) == Correctness.Correct
              Some(ItemResponseOutcome(mapping.mappedValue(response.value), isCorrect, outcomeProperties = getOutcomeProperties(isCorrect)))
            case None => {
              val isCorrect:Boolean = rd.isCorrect(responseValue) == Correctness.Correct
              Some(ItemResponseOutcome(if(isCorrect) 1 else 0, isCorrect, outcomeProperties = getOutcomeProperties(isCorrect)))
            }
          }
        }
        case None => None
      }
        //in this case, multiple responses are received, but this interaction only allows for one correct answer. choose the correct answer with the highest value
      case ArrayItemResponse(_,responseValues,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => {
            var max:Float = Float.MinValue;
            for (responseValue <- responseValues){
              val mappedValue = mapping.mappedValue(responseValue)
              if (max < mappedValue) max = mappedValue
            }
            Some(ItemResponseOutcome(max,true))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1,true))
          } else Some(ItemResponseOutcome(0,false))
        }
        case None => None
      }
      case _ => {
        Logger.error("received a response that was not a string response in TextEntryInteraction.getOutcome")
        None
      }
    }
  }
}

object TextEntryInteraction extends InteractionCompanion[TextEntryInteraction]{
  private val engine = new ScriptEngineManager().getEngineByName("JavaScript")

  def tagName = "textEntryInteraction"

  def apply(node: Node, itemBody:Option[Node]): TextEntryInteraction = {
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
      }
    )
  }
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "textEntryInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => TextEntryInteraction(node,Some(itemBody)))
    }
  }
  private def feedbackBlocks(itemBody:Node):Seq[FeedbackInline] = {
    (itemBody \\ "feedbackBlock").map(node => FeedbackInline(node,None))
  }
  private def expectedLength(n: Node): Int = (n \ "@expectedLength").text.toInt


}
