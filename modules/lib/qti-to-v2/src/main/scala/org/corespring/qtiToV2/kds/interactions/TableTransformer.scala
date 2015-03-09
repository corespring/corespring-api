package org.corespring.qtiToV2.kds.interactions

import scala.xml._
import scala.xml.transform._

/**
 * KDS are using an inline style on their tables but it doesn't really work, so we need to replace it with
 * inline-table.
 */
object TableTransformer {

  /**
   * The following two implicits come from http://stackoverflow.com/a/4334227/985323 for rewrite of scala.XML.Node
   * attributes.
   */
  private implicit def addGoodCopyToAttribute(attr: Attribute) = new {
    def goodcopy(key: String = attr.key, value: Any = attr.value): Attribute =
      Attribute(attr.pre, key, Text(value.toString), attr.next)
  }

  private implicit def iterableToMetaData(items: Iterable[MetaData]): MetaData = items match {
    case Nil => Null
    case head :: tail => head.copy(next = iterableToMetaData(tail))
  }

  def transform(node: Node) = new RuleTransformer(new RewriteRule {
    override def transform(node: Node) = node match {
      case elem: Elem if node.label == "table" =>
        elem.copy(attributes =
          for (attr <- elem.attributes) yield attr match {
            case attr@Attribute("style", _, _) =>
              attr.goodcopy(value = attr.value.text.replaceAll("inline;", "inline-table;"))
            case other => other
          }
        )
      case _ => node
    }
  }).transform(node).head.asInstanceOf[Elem]

}
