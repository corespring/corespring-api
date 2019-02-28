package org.corespring.container.client.controllers.helpers

import play.api.Logger
import play.api.libs.json.{ JsObject, JsValue }

import scala.concurrent.{ ExecutionContext, Future }
import scala.xml.Node

trait ItemInspector {
  /**
   * Return a list of components that are not declared in the xhtml
   *
   * @param xhtml
   * @param components
   * @return
   */
  def findComponentsNotInXhtml(xhtml: String, components: JsObject): Future[Seq[(String, JsValue)]]
}

class XmlItemInspector(processor: XhtmlProcessor, ec: ExecutionContext) extends ItemInspector {

  private lazy val logger = Logger(this.getClass)

  override def findComponentsNotInXhtml(xhtml: String, components: JsObject): Future[Seq[(String, JsValue)]] = Future {

    val xml = scala.xml.XML.loadString(processor.toWellFormedXhtml(xhtml))

    logger.trace(s"function=findComponentsNotInXhtml, xml=$xml")

    def hasId(id: String)(n: Node): Boolean = (n \ "@id").text == id
    def matchesComponentType(t: String)(n: Node) = n.label == t || (n \ s"@$t").length > 0

    def notInXhtml(keyValue: (String, JsValue)): Boolean = {
      val (id, comp) = keyValue
      val componentType = (comp \ "componentType").as[String]
      val nodeExists = (xml \\ "_").exists { n =>
        hasId(id)(n) && matchesComponentType(componentType)(n)
      }
      !nodeExists
    }
    components.fields.filter(notInXhtml)
  }(ec)
}
