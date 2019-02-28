package org.corespring.container.client.controllers.helpers

import org.corespring.container.client.views.txt.js.ComponentWrapper
import org.corespring.container.components.model.packaging.{ ClientDependencies, ClientSideDependency }
import org.corespring.container.components.model.{ Component, Interaction }
import org.corespring.container.utils.string
import play.api.libs.json._
import play.api.mvc.{ Result, Results }

trait NameHelper {

  protected def moduleName(org: String, comp: String) = string.join(".", org, comp)

  protected def tagName(org: String, comp: String): String = string.join("-", org, comp)

  protected def directiveName(org: String, comp: String) = s"$org${string.hyphenatedToTitleCase(comp)}"
}

trait LoadClientSideDependencies {

  def getClientSideDependencies(comps: Seq[Component]): Seq[ClientSideDependency] = {
    val packages = comps.map(_.packageInfo)
    val dependencies = packages.flatMap(p => (p \ "dependencies").asOpt[JsObject])
    dependencies.map(ClientDependencies(_)).flatten
  }
}

trait JsonHelper {

  def partialObj(fields: (String, Option[JsValue])*): JsObject =
    JsObject(fields.filter { case (_, v) => v.nonEmpty }.map { case (a, b) => (a, b.get) })

}
