package qti.models.interactions

import choices.{Choice, SimpleChoice}
import xml._
import models.{ArrayItemResponse, StringItemResponse, ItemResponseOutcome, ItemResponse}
import scala.Some
import xml.transform.{RewriteRule, RuleTransformer}
import scala.Null
import scala.Some
import xml.Text
import scala.Some
import qti.processors.FeedbackProcessor
import qti.models.ResponseDeclaration
import qti.models.QtiItem.Correctness
import controllers.Log
import testplayer.views.utils.QtiScriptLoader

case class ChoiceInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices{
  def getChoice(identifier: String):Option[Choice] = choices.find(_.identifier == identifier)
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
      case ArrayItemResponse(_,responseValues,_) => responseDeclaration match {
        case Some(rd) => rd.mapping match {
          case Some(mapping) => Some(ItemResponseOutcome(
            responseValues.foldRight[Float](0)((responseValue,sum) => sum + mapping.mappedValue(responseValue))
          ))
          case None => if (rd.isCorrect(response.value) == Correctness.Correct) {
            Some(ItemResponseOutcome(1))
          } else Some(ItemResponseOutcome(0))
        }
        case None => None
      }
      case _ => {
        Log.e("received a response that was not a string response in ChoiceInteraction.getOutcome")
        None
      }
    }
  }
}

object ChoiceInteraction extends InteractionCompanion[ChoiceInteraction]{

  def tagName = "choiceInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): ChoiceInteraction = ChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "simpleChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => ChoiceInteraction(node,Some(itemBody)))
    }
  }

  override def preProcessXml(interactionXml:Elem):NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(ChoiceInteraction(interactionXml,None)).transform(interactionXml)
  }

  override def getHeadHtml(toPrint:Boolean):String =
    InteractionHelper.getHeadHtml("choiceInteraction", toPrint) + "\n" + InteractionHelper.getHeadHtml("simpleChoice", toPrint)

}
