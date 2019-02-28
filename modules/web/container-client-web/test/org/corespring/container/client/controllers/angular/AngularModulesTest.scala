package org.corespring.container.client.controllers.angular

import org.corespring.container.components.model.{ Id, LayoutComponent, LibrarySource, Widget }
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class AngularModulesTest extends Specification with ComponentMaker with Mockito {

  val interaction = uiComp("comp", Nil)

  "createAngularModules" should {
    "add the default modules" in {
      val modules = new AngularModules("default")
      modules.createAngularModules(Nil, Nil) must_== Seq("default")
    }

    "add the component module for an interaction" in {
      val modules = new AngularModules()
      modules.createAngularModules(Seq(interaction), Nil) must_== Seq("org.comp")
    }

    "add the component module for an widget" in {
      val modules = new AngularModules()
      val widget: Widget = mock[Widget].id returns Id("org", "widget")
      modules.createAngularModules(Seq(widget), Nil) must_== Seq("org.widget")
    }

    "add the component module for a layout component" in {
      val modules = new AngularModules()
      val widget: LayoutComponent = mock[LayoutComponent].id returns Id("org", "layout")
      modules.createAngularModules(Seq(widget), Nil) must_== Seq("org.layout")
    }

    "add the component modules for a library with client src" in {
      val modules = new AngularModules()
      val library = lib("lib").copy(client = Seq(mock[LibrarySource]))
      modules.createAngularModules(Seq(library), Nil) must_== Seq("org.lib")
    }

    "not add the component modules for a library with no client src" in {
      val modules = new AngularModules()
      val library = lib("lib")
      modules.createAngularModules(Seq(library), Nil) must_== Nil
    }
  }
}
