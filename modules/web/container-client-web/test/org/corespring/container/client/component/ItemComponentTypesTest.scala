package org.corespring.container.client.component

import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.corespring.container.components.services.ComponentService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.JsValue
import play.api.libs.json.Json._

class ItemComponentTypesTest extends Specification with ComponentMaker with Mockito {

  val item = obj(
    "components" -> obj(
      "1" -> obj("componentType" -> "org-ui-1"),
      "2" -> obj("componentType" -> "org-w-one")),
    "xhtml" -> "<org-layout-1></org-layout-1>")

  class scope(item: JsValue = item)(comps: Component*) extends Scope {

    lazy val service = new ComponentService {
      override def components: Seq[Component] = comps
    }

    lazy val result = ItemComponentTypes.apply(service, item).map(_.componentType)
  }

  "apply" should {
    "return an empty list if there are no components" in new scope()() {
      result must_== Nil
    }

    "return an interaction" in new scope(item)(uiComp("ui-1", Seq.empty)) {
      result must_== Seq("org-ui-1")
    }

    "return widget types" in new scope(item)(widget("w-one")) {
      result must_== Seq("org-w-one")
    }

    "work with comps declared in layout" in new scope(item)(layout("layout-1")) {
      result must_== Seq("org-layout-1")
    }

    val itemWithMultipleRootNodesInXhtml = obj(
      "components" -> item \ "components",
      "xhtml" -> "<h1>headline</h1><org-layout-1></org-layout-1>")

    "work with multiple root nodes in xhtml" in new scope(itemWithMultipleRootNodesInXhtml)(layout("layout-1")) {
      result must_== Seq("org-layout-1")
    }
  }
}
