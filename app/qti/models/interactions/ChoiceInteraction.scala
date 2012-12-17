package qti.models.interactions

import choices.SimpleChoice
import xml.Node

case class ChoiceInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends Interaction {
  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
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
}
