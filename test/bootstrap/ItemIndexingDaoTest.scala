package bootstrap

import com.mongodb.casbah.Imports._
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item.Item
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
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

    lazy val underlyingDao = {
      val m = mock[VersioningDao[Item, VersionedId[ObjectId]]]
      m.update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) answers { (args, _) =>
        val argArray = args.asInstanceOf[Array[Object]]
        val id = argArray(0).asInstanceOf[VersionedId[ObjectId]]
        Right(id)
      }
      m.revertToVersion(any[VersionedId[ObjectId]]) answers { (args, _) =>
        val argArray = args.asInstanceOf[Array[Object]]
        argArray(0).asInstanceOf[VersionedId[ObjectId]]
      }
      m.save(any[Item], any[Boolean]) returns Right(itemId)
      m.insert(any[Item]) returns Some(itemId)
      m.findOneById(any[VersionedId[ObjectId]]) returns Some(item)
      m.delete(any[VersionedId[ObjectId]]) returns true
      m
    }

    val dao = new ItemIndexingDao(
      underlyingDao,
      itemIndexService,
      ExecutionContext.global)
  }

  "update" should {

    trait update extends scope {
      dao.update(itemId, query, true)
    }

    "trigger reindex" in new update {
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new update {
      there was one(itemIndexService).refresh()
    }

    "call update on dao" in new update {
      there was one(underlyingDao).update(itemId, query, createNewVersion)
    }

  }

  "revertToVersion" should {

    trait revertToVersion extends scope {
      dao.revertToVersion(itemId)
    }

    "trigger reindex" in new revertToVersion {
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new revertToVersion {
      there was one(itemIndexService).refresh()
    }

    "call revertToVersion on dao" in new revertToVersion {
      there was one(underlyingDao).revertToVersion(itemId)
    }

  }

  "save" should {

    trait save extends scope {
      dao.save(item, createNewVersion)
    }

    "trigger reindex" in new save {
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new save {
      there was one(itemIndexService).refresh()
    }

    "call save on dao" in new save {
      there was one(underlyingDao).save(item, createNewVersion)
    }

  }

  "insert" should {

    trait insert extends scope {
      dao.insert(item)
    }

    "trigger reindex" in new insert {
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new insert {
      there was one(itemIndexService).refresh()
    }

    "call insert on dao" in new insert {
      there was one(underlyingDao).insert(item)
    }

  }

  "findOneById" should {

    trait findOneById extends scope {
      dao.findOneById(itemId)
    }

    "not trigger reindex" in new findOneById {
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new findOneById {
      there was no(itemIndexService).refresh()
    }

    "call findOneById on dao" in new findOneById {
      there was one(underlyingDao).findOneById(itemId)
    }

  }

  "findOneCurrent" should {

    trait findOneCurrent extends scope {
      dao.findOneCurrent(query)
    }

    "not trigger reindex" in new findOneCurrent {
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new findOneCurrent {
      there was no(itemIndexService).refresh()
    }

    "call findOneById on dao" in new findOneCurrent {
      there was one(underlyingDao).findOneCurrent(query)
    }

  }

  "delete" should {

    trait delete extends scope {
      dao.delete(itemId)
    }

    "not trigger reindex" in new delete {
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new delete {
      there was no(itemIndexService).refresh()
    }

    "call delete on dao" in new delete {
      there was one(underlyingDao).delete(itemId)
    }

  }
}

