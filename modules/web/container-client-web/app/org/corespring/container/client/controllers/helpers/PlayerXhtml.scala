package org.corespring.container.client.controllers.helpers

import org.corespring.container.components.services.ComponentService
import org.htmlcleaner.{ TagNode, TagTransformation }

trait PlayerXhtml {
  def processXhtml(xhtml: String): String
}

class DefaultPlayerXhtml(componentService: ComponentService) extends PlayerXhtml {
  override def processXhtml(xhtml: String): String = {
    PlayerXhtml.mkXhtml(componentService.components.map(_.componentType), xhtml)
  }
}

private[helpers] object PlayerXhtml {
  def mkXhtml(components: Seq[String], xhtml: String): String = {

    /** <p> -> <div class="para"/> */
    def pToDiv = {
      val pToDiv = new TagTransformation("p", "div", true)
      pToDiv.addAttributeTransformation("class", "para ${class}")
      pToDiv
    }

    /** For IE8 support:  <custom-tag ...> -> <div custom-tag="" ...> */
    def tagToAttribute(t: String) = {
      val out = new TagTransformation(t, "div", true)
      out.addAttributeTransformation(t, "")
      out
    }

    /** a post processor to turn "para " to "para" */
    val cleanSpaceAfterPara = (n: TagNode) => {
      val divs = n.evaluateXPath("//div")
      divs.foreach((n: Object) => {
        import scala.collection.JavaConversions._
        val tag = n.asInstanceOf[TagNode]
        if (tag.getAttributeByName("class") == "para ") {
          tag.setAttributes(Map[String, String]("class" -> "para"))
        }
      })
    }

    val customTagToDivs = components.map(tagToAttribute)
    val postProcessors = Seq(cleanSpaceAfterPara)
    XhtmlProcessor.process(customTagToDivs :+ pToDiv, postProcessors, xhtml)
  }
}

