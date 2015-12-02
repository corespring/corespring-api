package org.corespring.services.salat

import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError, SalatInsertError, SalatRemoveError }
import org.bson.types.ObjectId
import org.corespring.errors.{ PlatformServiceError, CollectionInsertError, GeneralError }
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.services.item.ItemService

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class ContentCollectionServiceTest extends Specification with Mockito {

  trait scope extends Scope {
    val context = mock[Context]

    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.grantAccessToCollection(any[ObjectId], any[ObjectId], any[Permission]) returns {
        Success(Organization("mock"))
      }
      m
    }
    val itemService = mock[ItemService]
    val archiveConfig = mock[ArchiveConfig]

    val orgId = ObjectId.get
    val collection = new ContentCollection("test-collection", orgId)

    val dao = {
      val m = mock[SalatDAO[ContentCollection, ObjectId]]
      m.insert(collection) returns Some(collection.id)
      m
    }

    val shareItemsWithCollectionsService = {
      val m = mock[ShareItemWithCollectionsService]
      m
    }

    val item = new Item(collectionId = collection.id.toString)
    val service = new ContentCollectionService(
      dao,
      context,
      orgCollectionService,
      shareItemsWithCollectionsService,
      itemService,
      archiveConfig)
  }

  "insertCollection" should {
    "should return failure when dao fails to insert" in new scope {
      dao.insert(collection) returns None
      service.insertCollection(collection) must_== Failure(_: CollectionInsertError)
    }

    "should return failure when dao throws error" in new scope {
      dao.insert(collection) throws mock[SalatInsertError]
      service.insertCollection(collection) must_== Failure(_: CollectionInsertError)
    }

    "should return failure when orgCollectionService.grantAccessToCollection fails" in new scope {
      orgCollectionService.grantAccessToCollection(any[ObjectId], any[ObjectId], any[Permission]) returns Failure(PlatformServiceError("test"))
      service.insertCollection(collection) must_== Failure(_: CollectionInsertError)
    }

    "should return the collection" in new scope {
      service.insertCollection(collection) must_== Success(collection)
    }
  }

  "delete" should {
    "should fail when services throw SalatDAOUpdateError" in new scope {
      dao.removeById(collection.id) throws mock[SalatDAOUpdateError]
      service.delete(collection.id) must_== Failure(_: GeneralError)
    }

    "should fail when services throw SalatRemoveError" in new scope {
      dao.removeById(collection.id) throws mock[SalatRemoveError]
      service.delete(collection.id) must_== Failure(_: GeneralError)
    }
  }

}

