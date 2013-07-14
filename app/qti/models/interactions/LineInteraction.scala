package qti.models.interactions

import qti.models.{CorrectResponseLineEquation, CorrectResponseSingle, ResponseDeclaration}
import models.itemSession.{ArrayItemResponse, StringItemResponse, ItemResponseOutcome, ItemResponse}
import xml.Node
import qti.models.QtiItem.Correctness

case class LineInteraction(responseIdentifier: String) extends Interaction{

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    def outcomeProperties(responseValue:String):Map[String,Boolean] = {
      def toMap(value:String) =  if (responseValue == value) Map("correct" -> true) else Map("incorrect" -> true)
      responseDeclaration.map[Map[String,Boolean]](rd => rd.correctResponse match {
        case Some(CorrectResponseSingle(value)) => toMap(value)
        case Some(CorrectResponseLineEquation(value,_,_,_)) => toMap(value)
        case _ => Map()
      }).getOrElse(Map())
    }
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(
            ItemResponseOutcome(mapping.mappedValue(response.value),
            rd.isCorrect(responseValue) == Correctness.Correct,
            outcomeProperties = outcomeProperties(responseValue))
          )
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1,true,outcomeProperties = outcomeProperties(responseValue)))
          } else Some(ItemResponseOutcome(0,false,outcomeProperties = outcomeProperties(responseValue)))
        }
        case None => None
      }
      case ArrayItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => {
          val pta = responseValue(0).split(",").map(_.toDouble)
          val ptb = responseValue(1).split(",").map(_.toDouble)
          val slope = (pta(1) - ptb(1))/(pta(0) - ptb(0))
          val yintercept = pta(1) - (slope * pta(0))
          val equation = "y="+slope+"x+"+yintercept
          rd.mapping match {
            case Some(mapping) => Some(
              ItemResponseOutcome(mapping.mappedValue(equation),
                rd.isCorrect(equation) == Correctness.Correct,
                outcomeProperties = outcomeProperties(equation))
            )
            case None => if (rd.isCorrect(equation) == Correctness.Correct) {
              Some(ItemResponseOutcome(1,true,outcomeProperties = outcomeProperties(equation)))
            } else Some(ItemResponseOutcome(0,false,outcomeProperties = outcomeProperties(equation)))
          }
        }
        case None => None
      }
      case _ => {
        Logger.error("received a response that was not a string response in LineInteraction.getOutcome")
        None
      }
    }
  }

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
