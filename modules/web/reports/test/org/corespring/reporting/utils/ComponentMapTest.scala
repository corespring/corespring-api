package org.corespring.reporting.utils

import org.corespring.container.components.model.{Component, Id}
import org.corespring.container.components.loader.ComponentLoader
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

class ComponentMapTest extends Specification with Mockito {

  PlaySingleton.start()

  class scope extends Scope with ComponentMap {
    val org = "corespring"
    val componentNames = Map("multiple-choice" -> "Multiple Choice Component")
    val components = componentNames.map{ case (name, title) => new Component(Id(org, name), Json.obj("title" -> title)) }.toSeq
    override def componentLoader = new ComponentLoader {
      def all = components
      def reload = ???
    }
  }

  "componentMap" should {

    "return components from loader" in new scope {
      componentMap must beEqualTo(componentNames.map{ case (name, title) => (s"$org-$name" -> title) }.toMap)
    }

  }

}
