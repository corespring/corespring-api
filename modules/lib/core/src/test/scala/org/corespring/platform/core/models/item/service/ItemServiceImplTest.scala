package org.corespring.platform.core.models.item.service

import com.mongodb.DBObject
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.item.index.ItemIndexSearchResult
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import org.corespring.platform.core.models.item.{ TaskInfo, Item }
import org.corespring.platform.core.models.itemSession.{ DefaultItemSession, ItemSession }
import org.corespring.platform.core.services.item.{ItemIndexQuery, ItemIndexService, ItemVersioningDao, ItemServiceWired}
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.BaseTest
import org.corespring.test.utils.mocks.MockS3Service
import org.specs2.execute.Result
import org.specs2.mock.Mockito
import play.api.Play
import play.api.libs.json.Json
import se.radley.plugin.salat.SalatPlugin

import scala.concurrent._
import scalaz.Success

class ItemServiceImplTest extends BaseTest with Mockito {

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

  "save" should {

    def assertSaveWithStoredFile(name: String, shouldSucceed: Boolean): Result = {
      val mockS3: CorespringS3Service = new MockS3Service
      val s = new ItemServiceWired(mockS3, DefaultItemSession, ItemVersioningDao, itemIndexService)
      val id = VersionedId(ObjectId.get)
      val file = StoredFile(name, "image/png", false, StoredFile.storageKey(id, "data", name))
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
}
