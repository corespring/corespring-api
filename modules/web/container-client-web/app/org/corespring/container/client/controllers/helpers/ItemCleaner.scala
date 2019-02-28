package org.corespring.container.client.controllers.helpers

import play.api.libs.json._
import org.jsoup._

/**
 * A helper utility to remove components that don't have a representation in the markup
 */
object ItemCleaner {

  def cleanComponents(markup: String, components: JsObject) = {
    val parsedMarkup = Jsoup.parse(markup)
    def hasComponent(id: String) = Option(parsedMarkup.select(s"#${id}").first()).nonEmpty

    JsObject(components.fields.filter { case (id, _) => hasComponent(id) })
  }
}
