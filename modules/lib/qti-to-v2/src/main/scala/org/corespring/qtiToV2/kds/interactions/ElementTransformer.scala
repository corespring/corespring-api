package org.corespring.qtiToV2.kds.interactions

import org.corespring.qtiToV2.interactions.InteractionTransformer
import play.api.libs.json.JsObject

import scala.xml._

object ElementTransformer extends InteractionTransformer {

  val transforms = Map(
    <span class="under"/> -> <u/>)

  /**
   * Transforms elements types to other element types based on class + label matching of transforms map.
   */
  override def transform(node: Node, manifest: Node) = transforms.find { case (key, _) => node.matches(key) } match {
    case Some(pair) => pair._2.asInstanceOf[Elem].copy(child = node.child)
    case _ => node
  }

  implicit class NodeMatches(node: Node) {
    def matches(other: Node) = node.label == other.label &&
      ((other \ "@class").text).split(" ").toSet.subsetOf(((node \ "@class").text).split(" ").toSet)
  }

  override def interactionJs(qti: Node, manifest: Node) = Map.empty[String, JsObject]
}
