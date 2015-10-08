package org.corespring.qtiToV2.kds

import org.corespring.common.xml.XMLNamespaceClearer
import play.api.libs.ws.WS

import scala.collection._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.transform.{RuleTransformer, RewriteRule}
import scala.xml._

class ResponseProcessingTemplateConverter(get: (String => Node) = ResponseProcessingTemplateConverter.getXMLFromURL)
  extends XMLNamespaceClearer {

  var cache = mutable.HashMap[String, Node]()

  def withTemplate(node: Node) = {
    var url = (node \ "@template").text
    url.nonEmpty match {
      case true => cache.get(url) match {
        case Some(node) => node
        case _ => {
          val response = <responseProcessing>{ clearNamespace(get(url)).child }</responseProcessing>
          cache += (url -> response)
          response
        }
      }
      case _ => node
    }
  }

  implicit class AddTemplateConverterTools(node: Node) {

    def hasTemplate = node.attribute("template").nonEmpty

    def withTemplate = ResponseProcessingTemplateConverter.this.withTemplate(node)

    def substituting(substitution: (String, String)) = {
      new RuleTransformer(new RewriteRule {
        override def transform(n: Node): NodeSeq = n match {
          case e: Elem => (e \ "@identifier").text match {
            // I think this erases all other attributes, but I think this is ok response processing nodes, all of which
            // using identifiers seem to have that as the sole attribute.
            case substitution._1 => e % Attribute(null, "identifier", substitution._2, Null)
            case _ => e
          }
          case _ => n
        }
      }).transform(node).head
    }
  }

}


object ResponseProcessingTemplateConverter {
  def getXMLFromURL(url: String): Node = XML.loadString(Await.result(WS.url(url).get(), Duration.Inf).body)
}