package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.helpers.{ ItemInspector, PlayerXhtml }
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks._
import org.corespring.container.components.model.{ Component, Interaction }
import org.corespring.container.components.model.dependencies.ComponentMaker
import org.corespring.container.components.services.ComponentService
import org.corespring.test.TestContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class ItemTest extends Specification with Mockito with ComponentMaker with NoTimeConversions {

  trait IH extends CoreItemHooks with CreateItemHook

  class item(
    createError: Option[StatusMessage] = None,
    loadResult: JsValue = Json.obj("_id" -> Json.obj("$oid" -> "1"), "xhtml" -> "<div></div>"))
    extends Scope {

    def components: Seq[Component] = Seq.empty

    val hooks: ItemHooks = {
      val m = mock[ItemHooks]
      m.createItem(any[Option[String]])(any[RequestHeader]).returns {
        Future.successful {
          createError.map {
            e =>
              Left(e)
          }.getOrElse(Right("new_id"))
        }
      }
      m.createSingleComponentItem(any[Option[String]], any[String], any[String], any[JsObject])(any[RequestHeader]).returns {
        Future.successful {
          createError.map {
            e =>
              Left(e)
          }.getOrElse(Right("new_id"))
        }
      }
      m.load(any[String])(any[RequestHeader]).returns {
        Future.successful(Right((loadResult, Json.obj())))
      }
      m
    }

    val materialHooks = {
      val m = mock[ItemSupportingMaterialHooks]
      m
    }

    val componentService = {
      val m = mock[ComponentService]
      m.interactions returns components.flatMap {
        case i: Interaction => Some(i)
        case _ => None
      }
      m
    }

    val playerXhtml = {
      val m = mock[PlayerXhtml]
      m.processXhtml(any[String]) answers (s => s.asInstanceOf[String])
      m
    }

    lazy val itemInspector = {
      val m = mock[ItemInspector]
      m.findComponentsNotInXhtml(any[String], any[JsObject]) returns Future.successful(Seq.empty)
      m
    }

    val item = new Item(
      hooks,
      materialHooks,
      componentService,
      TestContext.containerContext,
      playerXhtml,
      itemInspector)

  }

  "Item" should {

    "load" should {
      s"return $OK" in new item {
        status(item.load("x")(FakeRequest("", ""))) === OK
      }

      "call playerXhtml.mkPlayerXhtml" in new item() {
        item.load("x")(FakeRequest("", ""))
        there was one(playerXhtml).processXhtml("<div></div>")
      }

      "prep the json" in new item(loadResult = Json.obj("_id" ->
        Json.obj("$oid" -> "1"), "xhtml" -> "<p>a</p>")) {
        val json = contentAsJson(item.load("x")(FakeRequest("", "")))
        (json \ "itemId").as[String] === "1"
        (json \ "xhtml").as[String] === """<p>a</p>"""
      }
    }

    "create returns error" in new item(createError = Some(UNAUTHORIZED -> "Error")) {
      val result = item.create(FakeRequest("", ""))
      status(result) === UNAUTHORIZED
      contentAsJson(result) === Json.obj("error" -> "Error")
    }

    "create" in new item {
      val result = item.create(FakeRequest("", ""))
      status(result) === OK
      contentAsJson(result) === Json.obj("itemId" -> "new_id")
    }

    trait createWithSingleComponent extends item {
      override lazy val components = Seq(uiComp("type", Nil).copy(defaultData = Json.obj("defaultData" -> true)))
      val request = FakeRequest("", "")
      lazy val result = item.createWithSingleComponent("org-type")(request)
    }

    "createWithSingleComponent" should {
      s"returns $CREATED when successful" in new createWithSingleComponent {
        status(result) === CREATED
      }

      s"returns $NOT_FOUND when the interaction can't be found" in new createWithSingleComponent {
        override lazy val components = Seq.empty
        status(result) === NOT_FOUND
      }

      s"returns $NOT_FOUND when the interaction defaultData isn't a JsObject" in new createWithSingleComponent {
        override lazy val components = Seq(uiComp("type", Nil).copy(defaultData = JsString("hi")))
        status(result) === NOT_FOUND
      }

      s"calls hooks.createSingleComponentItem" in new createWithSingleComponent {
        Await.result(result, 1.second)
        there was one(hooks).createSingleComponentItem(None, "org-type", "singleComponent", Json.obj("defaultData" -> true))(request)
      }

    }
  }
}
