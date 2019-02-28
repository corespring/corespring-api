package org.corespring.container.client.component

import org.corespring.container.client.controllers.helpers.{ NameHelper, XhtmlProcessor }
import org.corespring.container.components.model._
import org.corespring.container.components.services.ComponentService
import play.api.Logger
import play.api.libs.json.JsValue

object ItemComponentTypes extends NameHelper {

  private lazy val logger = Logger(ItemComponentTypes.getClass)

  def apply(componentService: ComponentService, item: JsValue): Seq[Component] = {
    apply(componentService.interactions, componentService.widgets, componentService.layoutComponents, item)
  }

  def apply(interactions: Seq[Interaction], widgets: Seq[Widget], layoutComponents: Seq[LayoutComponent], item: JsValue): Seq[Component] = {

    def layoutTypesInXml(xmlString: String, components: Seq[LayoutComponent]): Seq[String] = {

      import XhtmlProcessor._
      //Note: the xhtml may not have a single root - so we wrap it
      val xml = scala.xml.XML.loadString(toWellFormedXhtml(xmlString))

      val usedInXml = components.filter {
        lc =>
          val name = tagName(lc.org, lc.name)
          val hasComponentAsAttribute = xml.child.exists(!_.attribute(name).isEmpty)
          (xml \\ name).length > 0 || hasComponentAsAttribute
      }

      usedInXml.map(lc => tagName(lc.org, lc.name))
    }

    logger.debug(s"function=componentTypes - inspect json for types...")

    val types: Seq[String] = (item \ "components" \\ "componentType").map(_.as[String]).distinct

    def componentTypeMatches(t: String)(ci: ComponentInfo) = tagName(ci.id.org, ci.id.name) == t

    val validComponents = types.filter {
      t =>
        (interactions ++ widgets).exists(componentTypeMatches(t))
    }

    def layoutComponentsInItem: Seq[String] = {
      val out: Seq[String] = (item \ "xhtml").asOpt[String].map {
        l =>
          layoutTypesInXml(l, layoutComponents)
      }.getOrElse(Seq())
      out
    }
    val out = validComponents ++ layoutComponentsInItem
    logger.trace(s"function=componentTypes types=$out")
    out.flatMap(t =>
      (interactions ++ widgets ++ layoutComponents).find(_.componentType == t))
  }
}

