package player.views.models

import scala.xml.Node
import org.corespring.qti.models.QtiItem
import org.corespring.common.log.PackageLogging

/** A list of keys that describe features that this qti document uses */
case class QtiKeys(keys: Seq[String])

object QtiKeys extends PackageLogging {

  def apply(itemBody: Node) = {

    object matches {
      def attrAndValue(name: String, value: String): Boolean = (itemBody \\ ("@" + name)).find(_.text == value).isDefined
      def node(key: String): Boolean = (itemBody \\ key).size > 0
      def attr(key: String): Boolean = (itemBody \\ ("@" + key)).size > 0
      def nodeOrAttr(key: String): Boolean = node(key) || attr(key)
    }

    def buildKeys(itemBody: Node): Seq[String] = {
      def interactionKeys = QtiItem.interactionModels.filter(i => matches.node(i.tagName)).map(_.tagName).distinct

      def otherKeys = {
        val nodes = Seq(
          matches.nodeOrAttr("tabs") -> "tabs",
          matches.nodeOrAttr("cs-tabs") -> "tabs",
          matches.nodeOrAttr("math") -> "math",
          matches.nodeOrAttr("tex") -> "tex",
          matches.attrAndValue("class", "numbered-lines") -> "numberedLines"
        )
        nodes.filter(_._1).map(_._2).distinct
      }
      interactionKeys ++ otherKeys
    }
    new QtiKeys(buildKeys(itemBody))
  }

}
