package org.corespring.v2.player.customScoring

import java.io.File

import global.Global
import org.apache.commons.io.FileUtils
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.helpers.{ ItemHelper, CollectionHelper }
import org.corespring.it.scopes.{ WithV2SessionHelper, orgWithAccessToken }
import org.corespring.models.item.resource.{ Resource, VirtualFile }
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

class CustomScoringIntegrationTest extends IntegrationSpecification {

  "Old qti js is transformed and " should {

    "multiple-choice - works with v2 /load-outcome" in new testScope {
      val rootPath = "corespring-multiple-choice/one"
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "drag-and-drop - works with v2 /load-outcome" in new testScope {
      val rootPath = "corespring-drag-and-drop/one"
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "inline-choice - works with v2 /load-outcome" in new testScope {
      val rootPath = "corespring-inline-choice/one"
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 100, "note" -> "Overridden score"))
        status(r) === 200
      }.getOrElse(failure("load outcome failed"))
    }

    "line - works with v2 /load-outcome" in new testScope {
      val rootPath = "corespring-line/one"
      result.map { r =>
        (contentAsJson(r) \ "score" \ "summary").asOpt[JsObject] === Some(Json.obj("percentage" -> 30, "note" -> "Overridden score"))
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
  trait testScope extends orgWithAccessToken with WithV2SessionHelper {
    def rootPath: String
    def base = s"/custom-scoring/$rootPath"

    lazy val transformer = main.itemTransformer
    lazy val sessionService = main.sessionServices.main

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
      FileUtils.readFileToString(file)
    }

    private def seedItem(collectionId: ObjectId): VersionedId[ObjectId] = {
      val pathTwo = s"$base/qti.xml"
      val xmlString = loadResource(pathTwo)
      ItemHelper.create(collectionId, makeItem(pathTwo, xmlString))
    }

    private def makeItem(path: String, qti: String): Item = {
      Item(
        collectionId = collectionId.toString,
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
      logger.debug(s"custom scoring: ${ItemHelper.get(itemId).get.playerDefinition.get.customScoring}")
    }

    override def after: Unit = {
      super.after
      v2SessionHelper.delete(sessionId)
      CollectionHelper.delete(collectionId)
    }

    lazy val result = route(FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsJson(Json.obj())))
  }

}
