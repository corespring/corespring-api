package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component._
import org.corespring.container.client.controllers.EditorConfig
import org.corespring.container.client.hooks.EditorHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.{ ComponentEditorRenderer, EditorRenderer }
import org.corespring.container.client.views.models.{ ComponentsAndWidgets, MainEndpoints, SupportingMaterialsEndpoints }
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.corespring.container.components.services.ComponentService
import org.corespring.test.TestContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.Mode
import play.api.Mode.Mode
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.templates.Html
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

class BaseEditorTest extends Specification with Mockito with ComponentMaker with NoTimeConversions {

  trait scope extends Scope with BaseEditor[EditorHooks] {

    override val hooks = {
      val m = mock[EditorHooks]
      m.load(any[String])(any[RequestHeader]) returns Future(Right((Json.obj(), Json.obj())))
      m
    }

    val r = FakeRequest("", "")

    override lazy val config: EditorConfig = EditorConfig(Mode.Dev, true)

    override val componentEditorRenderer: ComponentEditorRenderer = mock[ComponentEditorRenderer]

    override val renderer: EditorRenderer = {
      val m = mock[EditorRenderer]
      m.render(any[MainEndpoints],
        any[SupportingMaterialsEndpoints],
        any[ComponentsAndWidgets],
        any[EditorClientOptions],
        any[ComponentsScriptBundle],
        any[Map[String, String]],
        any[Boolean],
        any[String],
        any[JsObject]) returns Future.successful(Html("<html></html>"))
    }

    override val bundler: ComponentBundler = {
      val m = mock[ComponentBundler]
      m.bundleAll(any[String], any[Option[String]], any[Boolean], any[Option[String]]) returns Some(
        ComponentsScriptBundle(Nil, Nil, Nil, Nil))
      m
    }
    override val componentJson: ComponentJson = new ComponentInfoJson("path")

    override val componentService: ComponentService = {
      val m = mock[ComponentService]
      m.interactions returns Nil
      m.widgets returns Nil
      m
    }

    override val endpoints: Endpoints = {
      val m = mock[Endpoints]
      m
    }

    override lazy val containerContext: ContainerExecutionContext = TestContext.containerContext

    override lazy val editorClientOptions: EditorClientOptions = EditorClientOptions(0, 8 * 1024, 500, StaticPaths.staticPaths)

    def waitFor[A](f: Future[A]): A = Await.result(f, 1.second)
  }

  "load" should {

    "call hooks.load" in new scope {
      load("id")(r)
      there was one(hooks).load("id")(r)
    }

    "pass SEE_OTHER from hook" in new scope {
      hooks.load(any[String])(any[RequestHeader]) returns Future(Left(SEE_OTHER, "other"))
      val result = load("id")(r)
      status(result) === SEE_OTHER
    }

    "show the error page" in new scope {
      hooks.load(any[String])(any[RequestHeader]) returns Future(Left(BAD_REQUEST -> "bad"))
      val result = load("itemId")(FakeRequest("", ""))
      status(result) === BAD_REQUEST
      contentAsString(result) === org.corespring.container.client.views.html.error.main(BAD_REQUEST, "bad", false).toString
    }

    trait componentsAndWidgets extends scope {

      componentService.interactions returns Seq(
        uiComp("released", Nil, released = true),
        uiComp("not-released", Nil, released = false))

      componentService.widgets returns Seq(
        widget("w-released", Nil, released = true),
        widget("w-not-released", Nil, released = false))

      waitFor(load("id")(r))
      lazy val captor = capture[ComponentsAndWidgets]
      there was one(renderer).render(any[MainEndpoints], any[SupportingMaterialsEndpoints], captor, any[EditorClientOptions], any[ComponentsScriptBundle], any[Map[String, String]], any[Boolean], any[String], any[JsObject])
    }

    "call renderer.renderJade with all components" in new componentsAndWidgets {
      override lazy val showNonReleased = true
      captor.value.components.as[Seq[JsValue]].length must_== 2
    }

    "call renderer.renderJade with only released components" in new componentsAndWidgets {
      override lazy val showNonReleased = false
      captor.value.components.as[Seq[JsValue]].length must_== 1
    }

    "call renderer.renderJade with all widgets if showNonReleased == false" in new componentsAndWidgets {
      override lazy val showNonReleased = false
      captor.value.widgets.as[Seq[JsValue]].length must_== 2
    }

    "call renderer.renderJade with all widgets if showNonReleased == true" in new componentsAndWidgets {
      override lazy val showNonReleased = true
      captor.value.widgets.as[Seq[JsValue]].length must_== 2
    }
  }

  "componentEditor" should {

    trait componentEditor extends scope {

      override def componentEditorResult(componentType: String, request: Request[AnyContent], defaults: JsValue) = {
        Future.successful(Ok("<html></html>"))
      }

      hooks.load(any[String])(any[RequestHeader]) returns {
        Future.successful(Right((Json.obj("components" -> Json.obj(
          "singleComponent" -> Json.obj("componentType" -> "type"))), Json.obj())))
      }
    }

    s"return $BAD_REQUEST if the component type can't be read from the json model" in new componentEditor {
      hooks.load(any[String])(any[RequestHeader]) returns {
        Future.successful((Right(Json.obj("components" -> Json.obj(
          "singleComponent" -> Json.obj())), Json.obj())))
      }
      val result = componentEditor("id")(r)
      status(result) must_== BAD_REQUEST
    }

    trait withError extends componentEditor {
      hooks.load(any[String])(any[RequestHeader]) returns {
        Future.successful(Left(402 -> "err"))
      }
    }

    s"return the error status code" in new withError {
      val result = componentEditor("id")(r)
      status(result) must_== 402
    }

    s"return the error body" in new withError {
      val result = componentEditor("id")(r)
      contentAsString(result) must_== org.corespring.container.client.views.html.error.main(402, "err", false).toString()
    }

    s"return $OK" in new componentEditor {
      val result = componentEditor("id")(r)
      status(result) must_== OK
    }

    s"return the html" in new componentEditor {
      val result = componentEditor("id")(r)
      contentAsString(result) must_== "<html></html>"
    }
  }
}
