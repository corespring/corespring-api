package org.corespring.v2.player.customScoring

import java.io.File

import common.seed.SeedDb
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ Resource, VirtualFile }
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, TaskInfo }
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.test.helpers.models.{ V2SessionHelper, CollectionHelper, ItemHelper }
import org.corespring.v2.player.scopes.orgWithAccessToken
import play.api.Play
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.xml.Node

class CustomScoringTest extends IntegrationSpecification {

  "Old qti js is transformed and " should {

    "multiple-choice - works with v2 /load-outcome" in new testScope("corespring-multiple-choice/one") {
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "drag-and-drop - works with v2 /load-outcome" in new testScope("corespring-drag-and-drop/one") {
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "inline-choice - works with v2 /load-outcome" in new testScope("corespring-inline-choice/one") {
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "line - works with v2 /load-outcome" in new testScope("corespring-line/one") {
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 60, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "text-entry - works with v2 /load-outcome" in new testScope("corespring-text-entry/one") {
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }
  }

  /**
   * This scope seeds the db with the test qti.xml
   * - runs a transformation
   * - creates a session that references the item
   * - the runs load outcome
   * - checks that the wrapped responseProcessing js runs and returns a response
   */
  class testScope(val rootPath: String) extends orgWithAccessToken {

    def base = s"../../../qtiToV2/customScoring/$rootPath"

    import org.corespring.mongo.json.services.MongoService

    lazy val collection = SeedDb.salatDb()(app)("v2.itemSessions")
    lazy val sessionService = new MongoService(collection)

    lazy val transformer = new ItemTransformer {
      override def itemService: ItemService = ItemServiceWired

      override def cache: ItemTransformationCache = new ItemTransformationCache {
        override def setCachedTransformation(item: Item, transformation: JsValue): Unit = {}

        override def removeCachedTransformation(item: Item): Unit = {}

        override def getCachedTransformation(item: Item): Option[JsValue] = None
      }
    }

    private def mkSession(itemId: VersionedId[ObjectId], path: String): JsObject = {
      val id = Json.obj("itemId" -> itemId.toString)
      val jsonString = loadResource(path)
      val testData = Json.parse(jsonString).as[JsObject]
      (testData \ "session").as[JsObject] ++ id
    }

    private def loadResource(p: String): String = {
      val url = this.getClass.getResource(p)
      require(url != null)
      val file = new File(url.toURI)
      require(file.exists)
      scala.io.Source.fromFile(file).getLines.mkString("\n")
    }

    private def seedItem(collectionId: ObjectId): VersionedId[ObjectId] = {
      val pathTwo = s"$base/qti.xml"
      val xmlString = loadResource(pathTwo)
      ItemHelper.create(collectionId, makeItem(pathTwo, xmlString))
    }

    private def makeItem(path: String, qti: String): Item = {
      Item(
        taskInfo = Some(TaskInfo(title = Some(s"Integration test item: $path"))),
        data = Some(Resource(name = "data", files = Seq(
          VirtualFile(name = "qti.xml", "text/xml", true, qti)))))
    }

    lazy val collectionId = CollectionHelper.create(orgId)
    lazy val itemId = seedItem(collectionId)
    lazy val jsonPath = s"$base/session.json"
    lazy val sessionId = sessionService.create(mkSession(itemId, jsonPath)).get
    lazy val call = org.corespring.container.client.controllers.resources.routes.Session.loadOutcome(sessionId.toString)

    override def before: Unit = {
      super.before
      transformer.updateV2Json(itemId)
    }

    override def after: Unit = {
      super.after
      V2SessionHelper.delete(sessionId)
      CollectionHelper.delete(collectionId)
    }

    lazy val result = route(FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsJson(Json.obj())))
  }

}
