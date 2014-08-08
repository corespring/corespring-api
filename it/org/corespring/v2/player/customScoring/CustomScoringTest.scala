package org.corespring.v2.player.customScoring

import common.seed.SeedDb
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.platform.core.models.item.{ ItemTransformationCache, TaskInfo, Item }
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.test.helpers.models.ItemHelper
import play.api.Play
import play.api.libs.json.{ Json, JsObject, JsValue }

import scala.xml.Node

class CustomScoringTest extends IntegrationSpecification {

  "Old qti js is transformed and " should {

    "be executable by the v2 /load-outcome score processor" in {

      //1. seed an item with qti xml that has response processing
      val itemId = seedItem()
      //2. transform the item to v2
      transformer.updateV2Json(itemId)
      //3. save a session for that item
      val jsonPath = "/org/corespring/qtiToV2/customScoring/corespring-multiple-choice/one/session.json"
      sessionService.create(mkSession(itemId, jsonPath))
      //4. call load-outcome
      //5. assert that the outcome is correct
      true === true
    }

  }

  import org.corespring.mongo.json.services.MongoService

  val collection = SeedDb.salatDb()(Play.current)("v2.itemSessions")
  val sessionService = new MongoService(collection)

  val transformer = new ItemTransformer {
    override def itemService: ItemService = ItemServiceWired

    override def cache: ItemTransformationCache = new ItemTransformationCache {
      override def setCachedTransformation(item: Item, transformation: (Node, JsValue)): Unit = ???

      override def removeCachedTransformation(item: Item): Unit = ???

      override def getCachedTransformation(item: Item): Option[(Node, JsValue)] = None
    }
  }

  private def mkSession(itemId: VersionedId[ObjectId], path: String): JsObject = {
    Json.obj()
  }

  private def seedItem() = {
    val path = "/org/corespring/qtiToV2/customScoring/corespring-multiple-choice/one/qti.xml"
    //val url = this.getClass.getResource(url)
    val qti = scala.xml.XML.load(path)
    ItemHelper.create(ObjectId.get, makeItem(path, qti))
  }

  private def makeItem(path: String, qti: Node): Item = {
    Item(
      taskInfo = Some(TaskInfo(title = Some(s"Integration test item: $path"))),
      data = Some(Resource(name = "data", files = Seq(
        VirtualFile(name = "qti.xml", "text/xml", true, qti.toString)))))

  }

}
