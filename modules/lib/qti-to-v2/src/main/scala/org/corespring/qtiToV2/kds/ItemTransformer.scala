package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.common.xml.XMLNamespaceClearer
import play.api.libs.json.JsValue

import scala.io.Source
import scala.xml._
import scala.xml.transform.{RuleTransformer, RewriteRule}

object ItemTransformer extends PassageTransformer {


  def transform(xmlString: String, manifestItem: ManifestItem, sources: Map[String, Source]): JsValue = {
    val passageXml = manifestItem.resources.filter(_.resourceType == ManifestResourceType.Passage).map(transformPassage(_)(sources).getOrElse("")).mkString
    QtiTransformer.transform(xmlString.toXML(passageXml))
  }

  /**
   * Maps some KDS QTI nodes to valid HTML nodes, and cleans up namespaces.
   */
  implicit class XMLCleaner(string: String) extends XMLNamespaceClearer {

    private val labelMap = Map("partBlock" -> "div", "partBody" -> "div", "selectedResponseParts" -> "div")

    def toXML(passageXml: String): Elem = {
      def stripCDataTags(xmlString: String) =
        StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))
      val xml = XML.loadString(stripCDataTags(string))
      clearNamespace(removeResponseProcessing(xml.copy(child = (XML.loadString("<div>" + passageXml + "</div>") ++ xml.child)))) match {
        case elem: Elem => elem
        case _ => throw new Exception("Types are wrong")
      }
    }

    def removeResponseProcessing(node: Node): Node = {
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node) = n match {
          case n: Node if (n.label == "responseProcessing") => Seq.empty
          case _ => n
        }
      }).transform(node).head
    }

  }

}
