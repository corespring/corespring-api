package org.corespring.qti.models.interactions

import org.corespring.platform.core.models.itemSession.{ArrayItemResponse, StringItemResponse, ItemResponseOutcome, ItemResponse}
import org.corespring.qti.models.QtiItem.Correctness
import org.corespring.qti.models.ResponseDeclaration
import xml.Node

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
      case ArrayItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => {
          def isCorrect(fn: (String,Int)=>Boolean):Boolean = {
            var count:Int = 0
            var correctAcc:Boolean = true;
            for (value <- responseValue){
              correctAcc = correctAcc && fn(value,count)
              count += 1
            }
            correctAcc
          }
          rd.mapping match {
            case Some(mapping) => {
              var sum:Float = 0;
              val correct = isCorrect((value,count) => {
                if (rd.isValueCorrect(value,Some(count))){
                  sum += mapping.mappedValue(value)
                  true
                } else false
              })
              Some(ItemResponseOutcome(sum,
                correct, outcomeProperties = if (correct) Map("correct" -> true) else Map("incorrect" -> true)
              ))
            }
            case None => {
              if (isCorrect((value,count) => rd.isValueCorrect(value,Some(count)))) {
                Some(ItemResponseOutcome(1, true, outcomeProperties = Map("correct" -> true)))
              } else Some(ItemResponseOutcome(0,false,outcomeProperties = Map("incorrect" -> true)))
            }
          }
        }
        case None => None
      }
      case _ => None
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