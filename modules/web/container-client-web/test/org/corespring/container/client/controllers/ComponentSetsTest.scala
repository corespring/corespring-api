package org.corespring.container.client.controllers

import java.util.concurrent.TimeUnit

import akka.util.Timeout
import org.corespring.container.client.component.{ ComponentsConfig, SourceGenerator }
import org.corespring.container.components.model.{ Component, Id, Library }
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.corespring.container.components.services.DependencyResolver
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.GlobalSettings
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ Action, EssentialAction, SimpleResult }
import play.api.test.Helpers._
import play.api.test.{ FakeApplication, FakeRequest }

import scala.concurrent.Future

class ComponentSetsTest extends Specification with ComponentMaker with Mockito {

  sequential

  class MockSourceGenerator(name: String) extends SourceGenerator {
    override def assetPath: String = ""

    override def less(components: Seq[Component], customColors: JsObject = Json.obj()): String = s"@a:3; .something { color: @a; }"

    override def js(components: Seq[Component]): String = s"$name - js - ${components.map(_.componentType).mkString(",")}"

    override protected def libraryToJs(l: Library): String = "libaryToJs"
  }

  val sets = new ComponentSets {

    val componentsConfig = ComponentsConfig("", "", "", false, false)

    override def playerGenerator: SourceGenerator = new MockSourceGenerator("player")

    override def editorGenerator: SourceGenerator = new MockSourceGenerator("editor")

    override def catalogGenerator: SourceGenerator = new MockSourceGenerator("catalog")

    override def allComponents: Seq[Component] = Seq(uiComp("name", Seq.empty))

    override val dependencyResolver: DependencyResolver = {
      val m = mock[DependencyResolver]
      m.resolveComponents(any[Seq[Id]], any[Option[String]]) returns allComponents
      m
    }

    override def resource[A >: EssentialAction](context: String, directive: String, suffix: String): A = Action {
      val (body, ct) = generateBodyAndContentType(context, directive, suffix)
      Ok(body).as(ct)
    }

    override def singleResource[A >: EssentialAction](context: String, componentType: String, suffix: String): A = Action {
      val (body, ct) = generate(context, Seq.empty, suffix)
      Ok(body).as(ct)
    }
  }

  object mockGlobal extends GlobalSettings

  "component sets" should {

    //Note: When running against essential action we need to
    //be running a Play app so a thread pool is available.
    //@see: https://github.com/playframework/playframework/issues/2011

    "return data" in running(FakeApplication(
      withGlobal = Some(mockGlobal))) {

      val result: Future[SimpleResult] = sets.resource("editor", "org[all]", "js")(FakeRequest("", "")).run
      contentAsString(result) === "editor - js - org-name"
    }

    "return data" in running(FakeApplication(withGlobal = Some(mockGlobal))) {
      val result: Future[SimpleResult] = sets.resource("player", "org[all]", "js")(FakeRequest("", "")).run
      contentAsString(result) === "player - js - org-name"
    }

    "compile less resources" in running(FakeApplication(withGlobal = Some(mockGlobal))) {
      val result: Future[SimpleResult] = sets.resource("player", "org[all]", "less")(FakeRequest("", "")).run
      contentAsString(result)(Timeout(30, TimeUnit.SECONDS)) must contain("color: 3")
    }

    "return js urls" in {
      sets.jsUrl("editor", Seq(uiComp("name", Seq.empty)), false) === Seq(org.corespring.container.client.controllers.routes.ComponentSets.resource("editor", "org[all]", "js").url)
    }

    "return js urls as a single resource" in {
      sets.jsUrl("editor", Seq(uiComp("name", Seq.empty)), true) === Seq(org.corespring.container.client.controllers.routes.ComponentSets.singleResource("editor", "org-name", "js").url)
    }

    "return js urls" in {
      sets.jsUrl("editor", Seq(uiComp("name", Seq.empty)), false) === Seq(org.corespring.container.client.controllers.routes.ComponentSets.resource("editor", "org[all]", "js").url)
    }

    "return less urls" in {
      sets.lessUrl("player", Seq(uiComp("name", Seq.empty)), false, Some("encodedColors")) === Seq(org.corespring.container.client.controllers.routes.ComponentSets.resource("player", "org[all]", "less").url + "?resourceToken=encodedColors")
    }

    "returns no url if no comps" in {
      sets.jsUrl("editor", Seq.empty, false) === Seq.empty
    }

    "returns no url if no comps" in {
      sets.lessUrl("editor", Seq.empty, false, None) === Seq.empty
    }

  }

}

