package org.corespring.qtiToV2.kds

import org.corespring.common.xml.XMLNamespaceClearer
import play.api.libs.ws.WS

import scala.collection._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.xml.{Elem, XML, Node}

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

  implicit class AddWithTemplate(node: Node) {
    def withTemplate = ResponseProcessingTemplateConverter.this.withTemplate(node)
  }

}


object ResponseProcessingTemplateConverter {

  def getXMLFromURL(url: String): Node = XML.loadString(Await.result(WS.url(url).get(), Duration.Inf).body)

}