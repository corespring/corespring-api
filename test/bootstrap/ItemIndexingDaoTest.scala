package bootstrap

import com.mongodb.CommandResult
import com.mongodb.casbah.Imports._
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Success

class ItemIndexingDaoTest extends Specification with Mockito {

  import ExecutionContext.Implicits.global

  val itemId = VersionedId(new ObjectId(), Some(0))
  val query = MongoDBObject("query" -> "stuff")

  val item = {
    val m = mock[Item]
    m.id returns itemId
    m
  }

  val createNewVersion = true

  class scope extends Scope {

    val itemIndexService = {
      val m = mock[ItemIndexService]
      m.reindex(any[VersionedId[ObjectId]]) returns Future { Success("") }
      m.refresh() returns Future { Success("") }
      m
    }

    type A2DBO = Any => DBObject
    lazy val mockCollection = {
      val m = mock[MongoCollection]

      val cr: CommandResult = mock[CommandResult].ok() returns true
      val wr: WriteResult = {
        val m = mock[WriteResult]
        m.getCachedLastError.returns(cr)
        m.getLastError.returns(cr)
        m
      }
      m.remove(any[Any], any[WriteConcern])(any[A2DBO], any[DBEncoder]) returns wr
      m
    }
    lazy val db = {
      val m = mock[MongoDB]
      m.apply(any[String]) returns mock[MongoCollection]
      m
    }

    lazy val context = new ServicesContext(this.getClass.getClassLoader)

    val itemIndexingDao = new ItemIndexingDao(
      db,
      context,
      "content",
      itemIndexService,
      ExecutionContext.global)

  }
  /*
  "update" should {

    "trigger reindex" in new scope {
      itemIndexingDao.update(itemId, query, true)
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new scope {
      itemIndexingDao.update(itemId, query, true)
      there was one(itemIndexService).refresh()
    }

  }

  "revertToVersion" should {

    "trigger reindex" in new scope {
      itemIndexingDao.revertToVersion(itemId)
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new scope {
      itemIndexingDao.revertToVersion(itemId)
      there was one(itemIndexService).refresh()
    }

  }

  "save" should {

    "trigger reindex" in new scope {
      itemIndexingDao.save(item, createNewVersion)
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new scope {
      itemIndexingDao.save(item, createNewVersion)
      there was one(itemIndexService).refresh()
    }

  }

  "insert" should {

    "trigger reindex" in new scope {
      itemIndexingDao.insert(item)
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new scope {
      itemIndexingDao.insert(item)
      there was one(itemIndexService).refresh()
    }

  }

  "findOneById" should {

    "not trigger reindex" in new scope {
      itemIndexingDao.findOneById(itemId)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new scope {
      itemIndexingDao.findOneById(itemId)
      there was no(itemIndexService).refresh()
    }

  }

  "findOneCurrent" should {

    "not trigger reindex" in new scope {
      itemIndexingDao.findOneCurrent(query)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new scope {
      itemIndexingDao.findOneCurrent(query)
      there was no(itemIndexService).refresh()
    }

  }

  "delete" should {

    "not trigger reindex" in new scope {
      itemIndexingDao.delete(itemId)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new scope {
      itemIndexingDao.delete(itemId)
      there was no(itemIndexService).refresh()
    }

  }

  */

}

