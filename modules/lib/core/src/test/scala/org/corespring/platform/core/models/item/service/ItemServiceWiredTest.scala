package org.corespring.platform.core.models.item.service

import com.mongodb.casbah.Imports._
import com.mongodb.{ WriteResult, WriteConcern, DBCollection, DBObject }
import com.mongodb.casbah.{ Imports, MongoCollection, MongoDB }
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.amazon.s3.models.DeleteResponse
import org.corespring.assets.CorespringS3Service
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.core.models.item.{ TaskInfo, Item }
import org.corespring.platform.core.models.itemSession.{ ItemSessionCompanion, DefaultItemSession, ItemSession }
import org.corespring.platform.core.services.item.{ ItemIndexQuery, ItemIndexService, ItemVersioningDao, ItemServiceWired }
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.BaseTest
import org.corespring.test.fakes.Fakes
import org.corespring.test.utils.mocks.MockS3Service
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.Play
import play.api.libs.json.Json
import se.radley.plugin.salat.SalatPlugin

import scala.concurrent._
import scalaz.Success

class ItemServiceWiredTest extends BaseTest with Mockito {

  import ExecutionContext.Implicits.global

  val s3: CorespringS3Service = mock[CorespringS3Service]
  val dao: SalatVersioningDao[Item] = mock[SalatVersioningDao[Item]]
  val itemIndexService = {
    import ExecutionContext.Implicits.global
    val m = mock[ItemIndexService]
    m.search(any[ItemIndexQuery]) returns Future { Success(ItemIndexSearchResult.empty) }
    m.reindex(any[VersionedId[ObjectId]]) returns Future { Success("") }
    m
  }

  val service = new ItemServiceWired(s3, DefaultItemSession, dao, itemIndexService)

  class serviceScope(val item: Item) extends Scope {

    val mockS3 = {
      val m = mock[CorespringS3Service]
      m.delete(any[String], any[String]) returns mock[DeleteResponse]
      m
    }

    val mockCollection = new Fakes.MongoCollection(1)

    val mockSession = mock[ItemSessionCompanion]
    val mockDao = {
      val m = mock[SalatVersioningDao[Item]]
      m.save(any[Item], any[Boolean]) returns Right(VersionedId(ObjectId.get, Some(0)))
      m.findOneById(any[VersionedId[ObjectId]]) returns Some(item)
      m.currentCollection returns mockCollection
      m
    }
    val mockIndex = {
      val m = mock[ItemIndexService]
      m.reindex(any[VersionedId[ObjectId]]) returns Future { Success("") }
      m
    }
    val service = new ItemServiceWired(mockS3, mockSession, mockDao, mockIndex)
  }

  val itemWithFiles: Item = {
    Item(
      data = Some(
        Resource(
          name = "data",
          files = Seq(
            StoredFile("good.png", "image/png", false, "key/good.png"),
            StoredFile("bad.png", "image/png", false, "key/bad.png")))))
  }

  "save" should {

    def assertSaveWithStoredFile(name: String, shouldSucceed: Boolean): Result = {
      val mockS3: CorespringS3Service = new MockS3Service
      val s = new ItemServiceWired(mockS3, DefaultItemSession, ItemVersioningDao, itemIndexService)
      val id = VersionedId(ObjectId.get, Some(0))
      val file = StoredFile(name, "image/png", false, StoredFile.storageKey(id.id, id.version.get, "data", name))
      val resource = Resource(name = "data", files = Seq(file))
      val item = Item(id = id, data = Some(resource), taskInfo = Some(TaskInfo(title = Some("original title"))))
      val latestId = s.insert(item)

      latestId.map { vid =>
        vid.version === Some(0)
      }.getOrElse(failure("insert failed"))

      val update = item.copy(id = latestId.get, taskInfo = Some(TaskInfo(title = Some("new title"))))

      s.save(update, true)

      val dbItem = s.findOneById(VersionedId(item.id.id))

      val expectedVersion = if (shouldSucceed) 1 else 0

      println("expecting version: " + expectedVersion)

      dbItem
        .map(i => i.id === VersionedId(id.id, Some(expectedVersion)))
        .getOrElse(failure("couldn't find item"))
    }

    "revert the version if a failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("bad.png", false)
    }

    "update the version if no failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("good.png", true)
    }

    "call s3.deleteFile for successful copies if another copy failed" in new serviceScope(itemWithFiles) {

      mockS3.copyFile(any[String], any[String], any[String]) answers { (args, mock) =>
        val arr = args.asInstanceOf[Array[Any]]
        val from = arr(1).asInstanceOf[String]
        val to = arr(2).asInstanceOf[String]

        if (from.contains("bad")) {
          throw new RuntimeException("error")
        } else {
          Unit
        }
      }

      service.save(item, true)
      val expectedKey = StoredFile.storageKey(item.id.id, item.id.version.get, "data", "good.png")
      there was one(mockS3).delete(AppConfig.assetsBucket, expectedKey)
    }
  }

  "session count" should {

    "count" in {

      val id = VersionedId(ObjectId.get)

      val item = Item(id = id,
        taskInfo = Some(TaskInfo(title = Some("just a test item"))))
      service.sessionCount(item) === 0

      val session = ItemSession(itemId = id)
      DefaultItemSession.insert(session)
      service.sessionCount(item) === 1

      DefaultItemSession.remove(session)

      service.sessionCount(item) === 0
    }

    "v2 session count" in {
      val id = VersionedId(ObjectId.get)
      val item = Item(id = id,
        taskInfo = Some(TaskInfo(title = Some("just a test item"))))
      service.v2SessionCount(item.id) === 0
      val session = MongoDBObject("itemId" -> item.id.toString)
      val db: MongoDB = Play.current.plugin[SalatPlugin].get.db()
      db("v2.itemSessions").insert(session)
      service.v2SessionCount(item.id) === 1
      db("v2.itemSessions").remove(MongoDBObject("itemId" -> item.id.toString))
      service.v2SessionCount(item.id) === 0
    }
  }

  "addFileToPlayerDefinition" should {
    "call collection.update with the correct operation and query" in new serviceScope(Item()) {

      import org.corespring.platform.core.models.mongoContext.context

      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(item, file)
      val expectedQuery = MongoDBObject("_id._id" -> item.id.id)
      mockCollection.queryObj === expectedQuery
      val fileDbo = com.novus.salat.grater[StoredFile].asDBObject(file)
      val expectedUpdate = MongoDBObject("$addToSet" -> MongoDBObject("data.playerDefinition.files" -> fileDbo))
      mockCollection.updateObj === expectedUpdate
    }
  }
}
