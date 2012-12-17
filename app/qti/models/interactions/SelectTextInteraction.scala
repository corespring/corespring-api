package qti.models.interactions

import xml.Node

case class SelectTextInteraction(responseIdentifier: String, selectionType: String, minSelection: Int, maxSelection: Int) extends Interaction {
  def getChoice(identifier: String) = None
}

object SelectTextInteraction extends InteractionCompanion[SelectTextInteraction]{
  def apply(node: Node,itemBody:Option[Node]): SelectTextInteraction = SelectTextInteraction(
    (node \ "@responseIdentifier").text,
    (node \ "@selectionType").text,
    (node \ "@minSelections").text.toInt,
    (node \ "@maxSelections").text.toInt
  )
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \ "selectTextInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => SelectTextInteraction(node,Some(itemBody)))
    }
  }
}
