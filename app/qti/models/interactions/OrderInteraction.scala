package qti.models.interactions

import choices.SimpleChoice
import xml.Node

case class OrderInteraction(responseIdentifier: String, choices: Seq[SimpleChoice]) extends Interaction {
  def getChoice(identifier: String) = choices.find(_.identifier == identifier)
}

object OrderInteraction extends InteractionCompanion[OrderInteraction]{
  def apply(node: Node, itemBody:Option[Node]): OrderInteraction = OrderInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "simpleChoice").map(SimpleChoice(_, (node \ "@responseIdentifier").text))
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \ "orderInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => OrderInteraction(node,Some(itemBody)))
    }
  }
}
