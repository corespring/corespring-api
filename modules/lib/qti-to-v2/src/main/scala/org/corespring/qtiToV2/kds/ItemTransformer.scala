package org.corespring.qtiToV2.kds

import java.util.zip.ZipFile

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.common.xml.XMLNamespaceClearer
import play.api.libs.json.JsValue

import scala.xml._
import scala.xml.transform.{RuleTransformer, RewriteRule}

object ItemTransformer {

  def transform(xmlString: String): JsValue = QtiTransformer.transform(xmlString.toXML)

  /**
   * Maps some KDS QTI nodes to valid HTML nodes, and cleans up namespaces.
   */
  implicit class XMLCleaner(string: String) extends XMLNamespaceClearer {

    private val labelMap = Map("partBlock" -> "div", "partBody" -> "div", "selectedResponseParts" -> "div")

    def toXML: Elem = {
      def stripCDataTags(xmlString: String) =
        StringEscapeUtils.unescapeHtml4("""(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1"))
      clearNamespace(removeResponseProcessing(XML.loadString(stripCDataTags(string)))) match {
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
