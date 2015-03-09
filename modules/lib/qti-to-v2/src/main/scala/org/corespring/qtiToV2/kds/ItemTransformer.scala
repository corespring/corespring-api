package org.corespring.qtiToV2.kds

import org.apache.commons.lang3.StringEscapeUtils
import org.corespring.common.xml.XMLNamespaceClearer
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.interactions.{TableTransformer, MathNotationTransformer, PathTransformer}
import play.api.libs.json.{JsObject, JsValue}

import scala.xml._
import scala.xml.transform.{RuleTransformer, RewriteRule}

object ItemTransformer extends PassageTransformer {

  def transform(xmlString: String, manifestItem: ManifestItem, sources: Map[String, SourceWrapper]): JsValue = {
    try {
      val passageXml = manifestItem.resources.filter(_.resourceType == ManifestResourceType.Passage)
        .map(transformPassage(_)(sources).getOrElse("")).mkString
      val xml = TableTransformer.transform(PathTransformer.transform(xmlString.toXML(passageXml)))
      QtiTransformer.transform(xml, sources)
    } catch {
      case e: Exception => {
        println(manifestItem.filename)
        throw e
      }
    }
  }

  /**
   * Maps some KDS QTI nodes to valid HTML nodes, and cleans up namespaces.
   */
  implicit class XMLCleaner(string: String) extends XMLNamespaceClearer {

    private val labelMap = Map("partBlock" -> "div", "partBody" -> "div", "selectedResponseParts" -> "div")

    def toXML(passageXml: String): Elem = {
      def stripCDataTags(xmlString: String) = """(?s)<!\[CDATA\[(.*?)\]\]>""".r.replaceAllIn(xmlString, "$1")
      val xml = XML.loadString(stripCDataTags(string))
      val stylesheets = (xml \ "stylesheet")
      clearNamespace(new RuleTransformer(new RewriteRule {
        override def transform(n: Node): NodeSeq = n match {
          case n: Elem if (n.label == "itemBody") => passageXml.nonEmpty match {
            case true => n.copy (child = stylesheets ++ XML.loadString (passageXml) ++ n.child)
            case _ => n.copy (child = stylesheets ++ n.child)
          }
          case _ => n
        }
      }).transform(xml).headOption.getOrElse(throw new Exception("There was no head element!"))).head.asInstanceOf[Elem]
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
