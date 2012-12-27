package qti.models.interactions

import choices.Choice
import xml.{Elem, NodeSeq, Node}
import play.api.libs.json.{JsString, JsObject, JsValue, Writes}
import qti.models.QtiItem.Correctness
import models.{ItemResponseOutcome, ItemResponse}
import qti.models.{QtiItem, ResponseDeclaration}

trait Interaction {
  val responseIdentifier: String
  //def getChoice(identifier: String): Option[Choice]
  def getOutcome(responseDeclaration: Option[ResponseDeclaration], response: ItemResponse) : Option[ItemResponseOutcome]
 // def getResponseDeclaration:Option[ResponseDeclaration] = None
  def validate(qtiItem:QtiItem):(Boolean, String) = {
    val isValid = !(qtiItem.responseDeclarations.find(_.identifier == responseIdentifier).isEmpty)
    val msg = if (isValid) "Ok" else "Missing response declartaion for " + responseIdentifier
    (isValid, msg)
  }
}

trait InteractionCompanion[T <: Interaction]{
  def apply(interaction:Node, itemBody:Option[Node]):T
  def parse(itemBody:Node):Seq[Interaction]
  def interactionMatch(e:Elem):Boolean
  def preProcessXml(interactionXml:Elem):NodeSeq = interactionXml
  def getHeadHtml(toPrint:Boolean):String
}

object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}