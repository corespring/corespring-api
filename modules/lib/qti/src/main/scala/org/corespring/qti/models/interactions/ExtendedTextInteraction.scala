package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.{ResponseOutcome, Response}
import org.corespring.qti.models.{ResponseDeclaration, QtiItem}
import xml.Node

case class ExtendedTextInteraction(responseIdentifier: String) extends Interaction {

  def isScoreable = false

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response) : Option[ResponseOutcome] = None

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
