package qti.models.interactions

import xml.{NodeSeq, Elem, Node}
import qti.models.ResponseDeclaration
import models.{ArrayItemResponse, StringItemResponse, ItemResponseOutcome, ItemResponse}
import qti.models.QtiItem.Correctness
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class TextEntryInteraction(responseIdentifier: String, expectedLength: Int, feedbackBlocks: Seq[FeedbackInline]) extends Interaction {
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = {
    response match {
      case StringItemResponse(_,responseValue,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(mapping.mappedValue(response.value)))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1))
          } else Some(ItemResponseOutcome(0))
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
            Some(ItemResponseOutcome(max))
          }
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1))
          } else Some(ItemResponseOutcome(0))
        }
        case None => None
      }
      case _ => {
        Log.e("received a response that was not a string response in TextEntryInteraction.getOutcome")
        None
      }
    }
  }
}

object TextEntryInteraction extends InteractionCompanion[TextEntryInteraction]{

  def interactionLabel = "textEntryInteraction"

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
