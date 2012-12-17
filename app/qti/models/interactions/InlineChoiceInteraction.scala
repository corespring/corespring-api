package qti.models.interactions

import choices.{InlineChoice, Choice}
import xml.Node

case class InlineChoiceInteraction(responseIdentifier: String, choices: Seq[InlineChoice]) extends Interaction {
  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
}

object InlineChoiceInteraction extends InteractionCompanion[InlineChoiceInteraction]{
  def apply(interaction: Node, itemBody:Option[Node]): InlineChoiceInteraction = InlineChoiceInteraction(

    (interaction \ "@responseIdentifier").text,
    (interaction \ "inlineChoice").map(InlineChoice(_, (interaction \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \ "inlineChoiceInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => InlineChoiceInteraction(node,Some(itemBody)))
    }
  }
}

