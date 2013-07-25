package qti.models.interactions

import models.itemSession.{ItemResponseOutcome, ItemResponse}
import qti.models.{QtiItem, ResponseDeclaration}
import xml.{Elem, NodeSeq, Node}
import common.log.PackageLogging

trait Interaction extends PackageLogging{
  val responseIdentifier: String
  protected val locked:Boolean = false;

  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse): Option[ItemResponseOutcome]

  /** Can this Interaction be automatically scored from the users response
    * Eg: multichoice can - but free written text can't be
    * @return
    */
  def isScoreable : Boolean

  def validate(qtiItem: QtiItem): (Boolean, String) = {
    val isValid = !qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty || locked
    val msg = if (isValid) "Ok" else "Missing response declartaion for " + responseIdentifier
    (isValid, msg)
  }
}

trait InteractionCompanion[T <: Interaction] {
  def tagName: String
  def apply(interaction: Node, itemBody: Option[Node]): T
  def parse(itemBody: Node): Seq[Interaction]
  def interactionMatch(e: Elem): Boolean = e.label == tagName
  def preProcessXml(interactionXml: Elem): NodeSeq = interactionXml
}

object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}
