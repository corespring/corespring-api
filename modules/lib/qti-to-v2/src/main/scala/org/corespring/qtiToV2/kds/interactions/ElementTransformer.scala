package org.corespring.qtiToV2.kds.interactions

import scala.xml._
import scala.xml.transform._

trait ElementTransformer {

  val transforms = Map(
      <span class="under"/> -> <u/>
  )

  /**
   * Transforms elements types to other element types based on class + label matching of transforms map.
   */
  def transformElements(node: Node) = new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = transforms.find{ case(key, _) => node.matches(key) } match {
      case Some(pair) => pair._2.asInstanceOf[Elem].copy(child = node.child)
      case _ => node
    }
  }).transform(node)

  implicit class NodeMatches(node: Node) {
    def matches(other: Node) = node.label == other.label &&
        ((other \ "@class").text).split(" ").toSet.subsetOf(((node \ "@class").text).split(" ").toSet)
  }
}
