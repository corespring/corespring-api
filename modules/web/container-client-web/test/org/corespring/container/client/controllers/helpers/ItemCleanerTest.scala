package org.corespring.container.client.controllers.helpers

import org.specs2.mutable.Specification
import play.api.libs.json._

class ItemCleanerTest extends Specification {

  "cleanComponents" should {

    val components = Seq("one", "two")
    val unreferencedComponents = Seq("two")
    val referencedComponents = components.diff(unreferencedComponents)

    val json = JsObject(
      components.map(id => id -> Json.obj()))

    val markup = <itemBody>
                   { referencedComponents.map(id => <corespring-multiple-choice id={ id }></corespring-multiple-choice>) }
                 </itemBody>.toString

    val result = ItemCleaner.cleanComponents(markup, json)

    "remove unreferenced components from JSON" in {
      result.keys must be equalTo (referencedComponents.toSet)
    }
  }

}
