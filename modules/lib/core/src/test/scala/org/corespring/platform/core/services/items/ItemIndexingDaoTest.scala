package org.corespring.platform.core.services.items

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ ItemIndexingDao, ItemIndexService }
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.{ Future, ExecutionContext }
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

  class daoScope extends Scope {

    val dao = {
      val m = mock[SalatVersioningDao[Item]]
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

    val itemIndexService = {
      val m = mock[ItemIndexService]
      m.reindex(any[VersionedId[ObjectId]]) returns Future { Success("") }
      m.refresh() returns Future { Success("") }
      m
    }

    val itemIndexingDao = new ItemIndexingDao(dao, itemIndexService)(ExecutionContext.global)

  }

  "update" should {

    "trigger reindex" in new daoScope() {
      itemIndexingDao.update(itemId, query, true)
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new daoScope() {
      itemIndexingDao.update(itemId, query, true)
      there was one(itemIndexService).refresh()
    }

    "call update on dao" in new daoScope() {
      itemIndexingDao.update(itemId, query, true)
      there was one(dao).update(itemId, query, createNewVersion)
    }

  }

  "revertToVersion" should {

    "trigger reindex" in new daoScope() {
      itemIndexingDao.revertToVersion(itemId)
      there was one(itemIndexService).reindex(itemId)
    }

    "trigger index refresh" in new daoScope() {
      itemIndexingDao.revertToVersion(itemId)
      there was one(itemIndexService).refresh()
    }

    "call revertToVersion on dao" in new daoScope() {
      itemIndexingDao.revertToVersion(itemId)
      there was one(dao).revertToVersion(itemId)
    }

  }

  "save" should {

    "trigger reindex" in new daoScope() {
      itemIndexingDao.save(item, createNewVersion)
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new daoScope() {
      itemIndexingDao.save(item, createNewVersion)
      there was one(itemIndexService).refresh()
    }

    "call save on dao" in new daoScope() {
      itemIndexingDao.save(item, createNewVersion)
      there was one(dao).save(item, createNewVersion)
    }

  }

  "insert" should {

    "trigger reindex" in new daoScope() {
      itemIndexingDao.insert(item)
      there was one(itemIndexService).reindex(item.id)
    }

    "trigger index refresh" in new daoScope() {
      itemIndexingDao.insert(item)
      there was one(itemIndexService).refresh()
    }

    "call insert on dao" in new daoScope() {
      itemIndexingDao.insert(item)
      there was one(dao).insert(item)
    }

  }

  "findOneById" should {

    "not trigger reindex" in new daoScope() {
      itemIndexingDao.findOneById(itemId)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new daoScope() {
      itemIndexingDao.findOneById(itemId)
      there was no(itemIndexService).refresh()
    }

    "call findOneById on dao" in new daoScope() {
      itemIndexingDao.findOneById(itemId)
      there was one(dao).findOneById(itemId)
    }

  }

  "findOneCurrent" should {

    "not trigger reindex" in new daoScope() {
      itemIndexingDao.findOneCurrent(query)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new daoScope() {
      itemIndexingDao.findOneCurrent(query)
      there was no(itemIndexService).refresh()
    }

    "call findOneById on dao" in new daoScope() {
      itemIndexingDao.findOneCurrent(query)
      there was one(dao).findOneCurrent(query)
    }

  }

  "delete" should {

    "not trigger reindex" in new daoScope() {
      itemIndexingDao.delete(itemId)
      there was no(itemIndexService).reindex(any[VersionedId[ObjectId]])
    }

    "not trigger index refresh" in new daoScope() {
      itemIndexingDao.delete(itemId)
      there was no(itemIndexService).refresh()
    }

    "call delete on dao" in new daoScope() {
      itemIndexingDao.delete(itemId)
      there was one(dao).delete(itemId)
    }

  }

}
