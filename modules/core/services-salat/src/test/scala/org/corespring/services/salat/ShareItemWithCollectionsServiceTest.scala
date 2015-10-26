package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAOUpdateError
import org.bson.types.ObjectId
import org.corespring.errors.{ ItemUnShareError, PlatformServiceError }
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Success, Failure }

class ShareItemWithCollectionsServiceTest extends Specification with Mockito {

  trait scope extends Scope {
    val context = mock[Context]
    val itemId = VersionedId(ObjectId.get, Some(0))
    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.isAuthorized(any[ObjectId], any[ObjectId], any[Permission]) returns true
      m
    }

    val dao = {
      val m = mock[VersioningDao[Item, VersionedId[ObjectId]]]
      m.update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) returns Right(itemId)
      m
    }

    val itemService = mock[ItemService]
    val service = new ShareItemWithCollectionsService(dao, itemService, orgCollectionService)

    val orgId = ObjectId.get
    val collection = ContentCollection("test-collection", orgId)
    val item = new Item(collectionId = collection.id.toString, id = itemId)
  }

  "shareItems" should {
    "should fail when orgCollectionService.isAuthorized is false" in new scope {
      orgCollectionService.isAuthorized(any[ObjectId], any[ObjectId], any[Permission]) returns false
      service.shareItems(orgId, Seq(item.id), collection.id) must_== Failure(_: PlatformServiceError)
    }
  }

  "unShareItems" should {

    "should fail when dao.update throws a SalatDAOUpdateError" in new scope {
      dao.update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) throws (mock[SalatDAOUpdateError])
      val result = service.unShareItems(orgId, Seq(item.id), collection.id)
      result must_== Failure(ItemUnShareError(Seq(item.id), Seq(collection.id)))
    }

    "should fail when dao.update throws any Exception" in new scope {
      dao.update(any[VersionedId[ObjectId]], any[DBObject], any[Boolean]) throws (new RuntimeException("!!"))
      val result = service.unShareItems(orgId, Seq(item.id), collection.id)
      result must_== Failure(ItemUnShareError(Seq(item.id), Seq(collection.id)))
    }.pendingUntilFixed

    "should return the item ids that were shared if successful" in new scope {
      service.unShareItems(orgId, Seq(item.id), collection.id) must_== Success(Seq(item.id))
    }
  }
}
