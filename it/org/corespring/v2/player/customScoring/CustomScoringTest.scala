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
import org.corespring.test.helpers.models.{ CollectionHelper, ItemHelper }
import org.corespring.v2.player.scopes.orgWithAccessToken
import play.api.Play
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.AnyContentAsJson
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.xml.Node

class CustomScoringTest extends IntegrationSpecification {

  "Old qti js is transformed and " should {

    "be executable by the v2 /load-outcome score processor" in new orgWithAccessToken {

      //1. seed an item with qti xml that has response processing
      val collectionId = CollectionHelper.create(orgId)
      val itemId = seedItem(collectionId)
      transformer.updateV2Json(itemId)
      val jsonPath = "../../../qtiToV2/customScoring/corespring-multiple-choice/one/session.json"
      val sessionId = sessionService.create(mkSession(itemId, jsonPath)).get
      val call = org.corespring.container.client.controllers.resources.routes.Session.loadOutcome(sessionId.toString)

      route(FakeRequest(call.method, s"${call.url}?access_token=$accessToken", FakeHeaders(), AnyContentAsJson(Json.obj()))).map {
        result =>
          status(result) === 200
      }.getOrElse(failure("load outcome failed"))
    }

  }

  import org.corespring.mongo.json.services.MongoService

  val collection = SeedDb.salatDb()(app)("v2.itemSessions")
  val sessionService = new MongoService(collection)

  val transformer = new ItemTransformer {
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
    val pathTwo = "../../../qtiToV2/customScoring/corespring-multiple-choice/one/qti.xml" //qtiToV2/customScoring/corespring-multiple-choice/one/qti.xml"
    println(this.getClass.getResource(pathTwo))
    val xmlString = loadResource(pathTwo)
    ItemHelper.create(collectionId, makeItem(pathTwo, xmlString))
  }

  private def makeItem(path: String, qti: String): Item = {
    Item(
      taskInfo = Some(TaskInfo(title = Some(s"Integration test item: $path"))),
      data = Some(Resource(name = "data", files = Seq(
        VirtualFile(name = "qti.xml", "text/xml", true, qti)))))

  }

}
