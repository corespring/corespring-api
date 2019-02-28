package org.corespring.container.client.controllers.resources

import org.corespring.container.client.controllers.helpers.{ ItemInspector, PlayerXhtml }
import org.corespring.container.client.controllers.resources.ItemDraft.Errors
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
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }

class ItemDraftTest extends Specification with Mockito with NoTimeConversions with ComponentMaker {

  trait DH extends CoreItemHooks with DraftHooks

  import ExecutionContext.Implicits.global

  trait scope extends Scope {

    val req = FakeRequest("", "")
    val hooks: DH = {
      val m = mock[DH] //.verbose
      m
    }

    def components: Seq[Component] = Seq.empty

    lazy val componentService = {
      val m = mock[ComponentService]
      m.components returns components
      m.interactions returns components.flatMap {
        case i: Interaction => Some(i)
        case _ => None
      }
      m
    }

    lazy val playerXhtml = {
      val m = mock[PlayerXhtml]
      m.processXhtml(any[String]) answers (s => s.asInstanceOf[String])
      m
    }

    lazy val materialHooks = {
      val m = mock[ItemDraftSupportingMaterialHooks]
      m
    }

    lazy val itemInspector = {
      val m = mock[ItemInspector]
      m.findComponentsNotInXhtml(any[String], any[JsObject]) returns Future.successful(Seq.empty)
      m
    }

    lazy val draft = new ItemDraft(
      TestContext.containerContext,
      componentService,
      hooks,
      playerXhtml,
      itemInspector,
      materialHooks)
  }

  "ItemDraft" should {

    "load" should {

      val json = Json.obj(
        "_id" -> Json.obj("$oid" -> "1"),
        "xhtml" -> "<div></div>")

      class load(loadResult: (JsValue, JsValue) = (json, Json.obj())) extends scope {
        hooks.load(any[String])(any[RequestHeader]).returns(Future.successful(Right(loadResult)))
      }

      s"return $OK" in new load {
        status(draft.load("x")(FakeRequest("", ""))) === OK
      }

      s"call playerXhtml.processXhtml" in new load {
        draft.load("x")(FakeRequest("", ""))
        there was one(playerXhtml).processXhtml("<div></div>")
      }

      val badJson = Json.obj("_id" ->
        Json.obj("$oid" -> "1"),
        "xhtml" -> "<p>a</p>")

      "prep the json" in new load(loadResult = (badJson, Json.obj())) {
        val json = contentAsJson(draft.load("x")(req))
        (json \ "itemId").as[String] === "1"
        (json \ "xhtml").as[String] === """<p>a</p>"""
      }
    }

    "createWithSingleComponent" should {

      trait createWithSingleComponent extends scope {

        val defaultData = Json.obj("defaultData" -> true)
        override def components = Seq(uiComp("type", Nil).copy(defaultData = defaultData))
        hooks.createSingleComponentItemDraft(any[Option[String]], any[String], any[String], any[JsObject])(any[RequestHeader]).returns {
          Future.successful(Right("itemId" -> "draftName"))
        }
      }

      "call hooks.createWithSingleComponent" in new createWithSingleComponent {
        Await.result(draft.createWithSingleComponent("org-type")(req), 1.second)
        there was one(hooks).createSingleComponentItemDraft(None, "org-type", SingleComponent.Key, defaultData)(req)
      }

      s"returns $CREATED" in new createWithSingleComponent {
        status(draft.createWithSingleComponent("org-type")(req)) === CREATED
      }

      s"returns $NOT_FOUND if it can't find the component" in new createWithSingleComponent {
        status(draft.createWithSingleComponent("org-type-two")(req)) === NOT_FOUND
      }

      s"returns itemId and draftName" in new createWithSingleComponent {
        contentAsJson(draft.createWithSingleComponent("org-type")(req)) === Json.obj("itemId" -> "itemId", "draftName" -> "draftName")
      }
    }

    "createItemAndDraft" should {

      trait createItemAndDraft extends scope {
        hooks.createItemAndDraft(any[Option[String]])(any[RequestHeader]).returns {
          Future.successful(Right("itemId" -> "draftName"))
        }
      }

      "call hooks.createItemAndDraft" in new createItemAndDraft {
        Await.result(draft.createItemAndDraft(req), 1.second)
        there was one(hooks).createItemAndDraft(None)(req)
      }

      s"returns $OK" in new createItemAndDraft {
        status(draft.createItemAndDraft(req)) === OK
      }

      s"returns itemId and draftName" in new createItemAndDraft {
        contentAsJson(draft.createItemAndDraft(req)) === Json.obj("itemId" -> "itemId", "draftName" -> "draftName")
      }
    }

    "saveSubset" should {

      class save(saveResult: JsValue = Json.obj()) extends scope {
        hooks.saveXhtml(anyString, anyString)(any[RequestHeader]) returns Future(Right(saveResult))
      }

      "fail to save if no json is supplied" in new save() {
        val result = draft.saveSubset("x", "xhtml")(FakeRequest())
        status(result) === BAD_REQUEST
        (contentAsJson(result) \ "error").as[String] === Errors.noJson
      }
    }

    "save" should {

      class save(saveResult: JsValue = Json.obj()) extends scope {
        hooks.save(anyString, any[JsValue])(any[RequestHeader]) returns Future(Right(saveResult))
      }

      "fail to save if no json is supplied" in new save() {
        val result = draft.save("x")(FakeRequest())
        status(result) must be equalTo (BAD_REQUEST)
        (contentAsJson(result) \ "error").as[String] must be equalTo (Errors.noJson)
      }

      val json = Json.obj("this" -> "is", "item" -> "json")

      "return 200 on success" in new save(json) {
        val result = draft.save("x")(FakeRequest().withJsonBody(json))
        status(result) must be equalTo (OK)
      }

      "return result from DraftHooks#save on success" in new save(json) {
        val result = draft.save("x")(FakeRequest().withJsonBody(json))
        contentAsJson(result) must be equalTo (json)
      }

      val json2 = Json.obj("xhtml" -> "", "components" -> Json.obj("0" -> Json.obj("name" -> "component0")))

      "clean out orhapned items from the json" in new save(json2) {
        val result = draft.save("x")(FakeRequest().withJsonBody(json2))
        val cleanedJson = json2 ++ Json.obj("components" -> Json.obj())
        contentAsJson(result) must be equalTo (cleanedJson)
      }

    }

    "commit" should {

      class commit(commitResult: Future[Either[StatusMessage, JsValue]] = Future(Right(Json.obj()))) extends scope {
        hooks.commit(anyString, any[Boolean])(any[RequestHeader]) returns commitResult
      }

      "set force to false" in new commit() {
        val fr = FakeRequest("", "")
        draft.commit("draftId")(fr)
        there was one(draft.hooks).commit("draftId", false)(fr)
      }

      "set force to true" in new commit() {
        val fr = FakeRequest("", "?force=true")
        draft.commit("draftId")(fr)
        there was one(draft.hooks).commit("draftId", true)(fr)
      }

    }

  }
}
