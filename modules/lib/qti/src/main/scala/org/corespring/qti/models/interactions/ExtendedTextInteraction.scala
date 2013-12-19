package org.corespring.qti.models.interactions

import org.corespring.qti.models.responses.{StringResponse, ResponseOutcome, Response}
import org.corespring.qti.models.{ ResponseDeclaration, QtiItem }
import xml.Node
import play.api.libs.json.JsValue
import org.corespring.qti.models.responses.javascript.DefaultResponseConverter

case class ExtendedTextInteraction(responseIdentifier: String) extends Interaction with DefaultResponseConverter {

  def isScoreable = false

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: Response): Option[ResponseOutcome] = None

  override def validate(qtiItem: QtiItem): (Boolean, String) = (true, "Ok")

}

object ExtendedTextInteraction extends InteractionCompanion[ExtendedTextInteraction] {

  def tagName = "extendedTextInteraction"

  def apply(node: Node, itemBody: Option[Node] = None): ExtendedTextInteraction = {
    val responseIdentifier = Interaction.responseIdentifier(node)
    ExtendedTextInteraction(responseIdentifier = responseIdentifier)
  }

  def parse(itemBody: Node): Seq[Interaction] = {
    val interactions = (itemBody \\ "extendedTextInteraction")
    if (interactions.isEmpty) {
      Seq()
    } else {
      interactions.map(node => ExtendedTextInteraction(node, Some(itemBody)))
    }
  }

}
