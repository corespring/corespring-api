package org.corespring.container.client.controllers.helpers

import org.corespring.container.components.model.LayoutComponent

@deprecated("use ItemTypeReader", "")
trait LayoutComponentReading { self: NameHelper =>

  def layoutTypesInXml(xmlString: String, components: Seq[LayoutComponent]): Seq[String] = {

    val xml = scala.xml.XML.loadString(xmlString)

    val usedInXml = components.filter { lc =>
      val name = tagName(lc.org, lc.name)

      val hasComponentAsAttribute = xml.child.exists(!_.attribute(name).isEmpty)

      (xml \\ name).length > 0 || hasComponentAsAttribute
    }

    usedInXml.map(lc => tagName(lc.org, lc.name))
  }
}
