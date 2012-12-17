package qti.models.interactions

import choices.Choice
import xml.{Elem, Node}
import play.api.libs.json.{JsString, JsObject, JsValue, Writes}
import qti.models.QtiItem.Correctness

trait Interaction {
  val responseIdentifier: String
  def getChoice(identifier: String): Option[Choice]
}
trait InteractionCompanion[T <: Interaction]{
  def apply(interaction:Node, itemBody:Option[Node]):T;
  def parse(itemBody:Node):Seq[Interaction];
}
object Interaction {
  def responseIdentifier(n: Node) = (n \ "@responseIdentifier").text
}