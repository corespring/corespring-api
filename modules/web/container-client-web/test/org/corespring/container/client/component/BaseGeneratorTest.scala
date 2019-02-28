package org.corespring.container.client.component

import org.corespring.container.client.component.SourceGenerator.Keys._
import org.corespring.container.components.model._
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.Json

class BaseGeneratorTest extends Specification with ComponentMaker {

  class generatorScope(libJs: String = "libJs") extends Scope {

    val generator = new BaseGenerator with JsStringBuilder {

      override def assetPath: String = ""
      override protected def libraryToJs(l: Library): String = libJs

      override def resource(path: String): Option[String] = {
        Some(s"resource-$path")
      }

      override def loadLibrarySource(path: String): Option[String] = Some(s"lib-$path")

      override def buildJsString(e: (String, String)*): String = e.foldLeft("")((acc, s) => acc + s)
    }
  }

  "BaseGenerator" should {

    "with 1 interaction, 1 widget and 1 layout components" in new generatorScope() {

      val comps = Seq(
        uiComp("one", Seq.empty).copy(client = Client("render", "configure", None)),
        widget("two", Seq.empty).copy(client = Client("render", "configure", None)),
        layout("three").copy(client = Seq(LibrarySource("a", "a"))))
      generator.js(comps) === generator.buildJsString(
        LocalLibs -> "",
        ThirdParty -> "",
        Libraries -> "",
        Interactions -> generator.interactionToJs(comps(0).asInstanceOf[Interaction]),
        Widgets -> generator.widgetToJs(comps(1).asInstanceOf[Widget]),
        Layout -> generator.layoutToJs(comps(2).asInstanceOf[LayoutComponent]))
    }

    "work with external srcs" in new generatorScope() {

      val comp = uiComp("one", Seq.empty)
        .copy(client = Client("render", "configure", None),
          packageInfo = Json.obj(
            "dependencies" -> Json.obj(
              "client" -> Json.obj(
                "dep-1" -> Json.obj("file" -> "path/to/dep-1.js"))),
            "libs" -> Json.obj(
              "client" -> Json.obj(
                "local-lib" -> Json.arr("a.js")))))

      val comps = Seq(comp)

      generator.js(comps) === generator.buildJsString(
        LocalLibs -> generator.loadLibrarySource("org/one/libs/a.js").get,
        ThirdParty -> generator.resource("dep-1/path/to/dep-1.js").get,
        Libraries -> "",
        Interactions -> generator.interactionToJs(comps(0)),
        Widgets -> "",
        Layout -> "")

    }

    "compiles component less" in new generatorScope() {
      val comp = uiComp("one", Seq.empty)
      val comps = Seq(comp)

      generator.less(comps).contains("@correct-background") === true
      generator.less(comps, Json.obj("correct-background" -> "#abcdef")).contains("@correct-background: #abcdef") === true
    }

    "only loads libs and dependencies once - if the paths are the same" in new generatorScope() {

      val comp = uiComp("one", Seq.empty)
        .copy(client = Client("render", "configure", None),
          packageInfo = Json.obj(
            "dependencies" -> Json.obj(
              "client" -> Json.obj(
                "dep-1" -> Json.obj("file" -> "path/to/dep-1.js"))),
            "libs" -> Json.obj(
              "client" -> Json.obj(
                "local-lib" -> Json.arr("a.js")))))

      val comps = Seq(comp, comp)

      val interactionJs = generator.interactionToJs(comps(0))

      generator.js(comps) === generator.buildJsString(
        LocalLibs -> generator.loadLibrarySource("org/one/libs/a.js").get,
        ThirdParty -> generator.resource("dep-1/path/to/dep-1.js").get,
        Libraries -> "",
        Interactions -> s"$interactionJs\n$interactionJs",
        Widgets -> "",
        Layout -> "")

    }
  }
}
