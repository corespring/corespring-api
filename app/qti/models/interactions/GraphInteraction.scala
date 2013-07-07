package qti.models.interactions

import qti.models.ResponseDeclaration
import models.itemSession.{ItemResponseOutcome, ItemResponse}
import xml.Node

case class GraphInteraction(responseIdentifier: String) extends Interaction{

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome] = {
    None
  }

  /** Can this Interaction be automatically scored from the users response
    * Eg: multichoice can - but free written text can't be
    * @return
    */
  def isScoreable: Boolean = false
}
object GraphInteraction extends InteractionCompanion[GraphInteraction]{
  def tagName: String = "graphInteraction"

  def apply(interaction: Node, itemBody: Option[Node]): GraphInteraction = {
    GraphInteraction(
      (interaction \ "@responseIdentifier").text
    )
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ tagName)
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => GraphInteraction(node, Some(itemBody)))
    }
  }
}
