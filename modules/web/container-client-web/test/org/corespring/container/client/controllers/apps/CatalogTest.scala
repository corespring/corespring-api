package org.corespring.container.client.controllers.apps

import org.corespring.container.client.component.{ ComponentBundler, ComponentsScriptBundle }
import org.corespring.container.client.hooks.CatalogHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.CatalogRenderer
import org.corespring.container.client.views.models.{ MainEndpoints, SupportingMaterialsEndpoints }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Mode
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.RequestHeader
import play.api.templates.Html
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CatalogTest extends Specification with Mockito {

  trait scope extends Scope {

    def mode = Mode.Test

    lazy val hooks = {
      val m = mock[CatalogHooks]
      m.showCatalog(any[String])(any[RequestHeader]) returns Future.successful(Right(Json.obj()))
      m
    }

    lazy val catalogRenderer = {
      val m = mock[CatalogRenderer]
      m.render(any[ComponentsScriptBundle], any[MainEndpoints], any[SupportingMaterialsEndpoints], any[Map[String, String]], any[Boolean], any[String], any[JsObject]) returns {
        Future.successful(Html("<catalog/>"))
      }
      m
    }

    lazy val bundler = {
      val m = mock[ComponentBundler]
      m.bundleAll(any[String], any[Option[String]], any[Boolean], any[Option[String]]) returns {
        Some(ComponentsScriptBundle(Nil, Nil, Nil, Nil))
      }
      m
    }

    lazy val req = FakeRequest()

    lazy val catalog = new Catalog(
      mode,
      hooks,
      catalogRenderer,
      bundler,
      containerContext = ContainerExecutionContext.TEST)
  }

  "load" should {
    "return an error from the hook" in new scope {
      hooks.showCatalog(any[String])(any[RequestHeader]) returns Future(Left(505 -> "No"))
      val result = catalog.load("id")(req)
      status(result) must_== 505
    }

    "return an error if the bundle fails" in new scope {
      bundler.bundleAll(any[String], any[Option[String]], any[Boolean], any[Option[String]]) returns {
        None
      }
      val result = catalog.load("id")(req)
      status(result) must_== BAD_REQUEST
    }

    "return the html" in new scope {
      val result = catalog.load("id")(req)
      contentAsString(result) must_== "<catalog/>"
    }
  }

}
