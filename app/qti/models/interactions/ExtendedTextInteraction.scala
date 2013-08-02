package qti.models.interactions

import org.corespring.platform.core.models.itemSession.{ItemResponseOutcome, ItemResponse}
import qti.models.{QtiItem, ResponseDeclaration}
import xml.Node

case class ExtendedTextInteraction(responseIdentifier: String) extends Interaction {

  def isScoreable = false

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome] = None

  override def validate(qtiItem: QtiItem): (Boolean, String) = (true, "Ok")
}

object ExtendedTextInteraction extends InteractionCompanion[ExtendedTextInteraction]{
  def tagName = "extendedTextInteraction"

  def apply(node: Node, itemBody:Option[Node]): ExtendedTextInteraction = {
    val responseIdentifier = Interaction.responseIdentifier(node)
    ExtendedTextInteraction(responseIdentifier = responseIdentifier)
  }
  def parse(itemBody:Node):Seq[Interaction] = {
    val interactions = (itemBody \\ "extendedTextInteraction")
    if (interactions.isEmpty){
      Seq()
    }else{
      interactions.map(node => ExtendedTextInteraction(node,Some(itemBody)))
    }
  }
}
