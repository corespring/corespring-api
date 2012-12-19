package qti.models.interactions

import choices.{Choice, SimpleChoice}
import xml._
import models.{ItemResponseOutcome, ItemResponse}
import scala.Some
import xml.transform.{RewriteRule, RuleTransformer}
import scala.Null
import scala.Some
import xml.Text
import scala.Some
import qti.processors.FeedbackProcessor

case class ChoiceInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends InteractionWithChoices{
  def getChoice(identifier: String):Option[Choice] = choices.find(_.identifier == identifier)
}

object ChoiceInteraction extends InteractionCompanion[ChoiceInteraction]{
  def apply(interaction: Node, itemBody: Option[Node]): ChoiceInteraction = ChoiceInteraction(
    (interaction \ "@responseIdentifier").text,
    (interaction \ "simpleChoice").map(SimpleChoice(_, (interaction \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \ "choiceInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => ChoiceInteraction(node,Some(itemBody)))
    }
  }
  def interactionMatch(e:Elem) = e.label == "choiceInteraction"
  override def preProcessXml(interactionXml:Elem):NodeSeq = {
    new InteractionProcessing.FeedbackOutcomeIdentifierInserter(ChoiceInteraction(interactionXml,None)).transform(interactionXml)
  }
}
