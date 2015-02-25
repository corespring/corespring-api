package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.interactions.{PathTransformer, FractionTransformer}
import play.api.libs.json.{JsObject, JsValue}

import scala.xml._
import scala.xml.transform.{RuleTransformer, RewriteRule}

object ItemTransformer extends PassageTransformer {

  def transform(xmlString: String, manifestItem: ManifestItem, sources: Map[String, SourceWrapper]): JsValue = {
    val passageXml = manifestItem.resources.filter(_.resourceType == ManifestResourceType.Passage)
      .map(transformPassage(_)(sources).getOrElse("")).mkString
    QtiTransformer.transform(PathTransformer.transform(xmlString.toXML(passageXml)))
  }

  /**
   * Maps some KDS QTI nodes to valid HTML nodes, and cleans up namespaces.
   */
  implicit class XMLCleaner(string: String) extends XMLNamespaceClearer with FractionTransformer {

    private val labelMap = Map("partBlock" -> "div", "partBody" -> "div", "selectedResponseParts" -> "div")

    def toXML(passageXml: String): Elem = {
      def stripCDataTags(xmlString: String) = """(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1")
      val xml = XML.loadString(stripCDataTags(string))
      transformFractions(clearNamespace(new RuleTransformer(new RewriteRule {
        override def transform(n: Node): NodeSeq = n match {
          case n: Elem if (n.label == "itemBody" && passageXml.nonEmpty) =>
            n.copy(child = XML.loadString(passageXml) ++ n.child)
          case _ => n
        }
      }).transform(xml).headOption.getOrElse(throw new Exception("There was no head element!")))).asInstanceOf[Elem]
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
