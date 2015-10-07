package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.{SalatRemoveError, SalatDAOUpdateError, SalatInsertError, SalatDAO}
import org.bson.types.ObjectId
import org.corespring.models.item.Item
import org.corespring.models.{ContentCollRef, ContentCollection}
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors._
import org.corespring.services.item.ItemService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{Failure, Success}

class ContentCollectionServiceTest extends Specification with Mockito {

  trait scope extends Scope {
    val dao = mock[SalatDAO[ContentCollection, ObjectId]]
    val context = mock[Context]
    val orgService = mock[OrganizationService]
    val itemService = mock[ItemService]
    val archiveConfig = mock[ArchiveConfig]
    val service = new ContentCollectionService(dao, context, orgService, itemService, archiveConfig)

    val orgId = ObjectId.get
    val collection = new ContentCollection("test-collection", orgId)
    var item = new Item(collectionId = collection.id.toString)
  }

  "insertCollection" should {
    "should fail when dao fails to insert" in new scope {
      dao.insert(collection) returns None

      service.insertCollection(orgId, collection, Permission.Write) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[CollectionInsertError]
      }
    }

    "should fail when dao throws error" in new scope {
      dao.insert(collection) throws mock[SalatInsertError]

      service.insertCollection(orgId, collection, Permission.Write) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[CollectionInsertError]
      }
    }

    "should fail when organization cannot be updated" in new scope {
      dao.insert(collection) returns Some(collection.id)
      orgService.addCollectionReference(any[ObjectId], any[ContentCollRef]) returns Failure(PlatformServiceError("test"))

      service.insertCollection(orgId, collection, Permission.Write) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[OrganizationAddCollectionError]
      }
    }

    "should fail when organization cannot be updated" in new scope {
      dao.insert(collection) returns Some(collection.id)
      orgService.addCollectionReference(any[ObjectId], any[ContentCollRef]) throws mock[SalatDAOUpdateError]

      service.insertCollection(orgId, collection, Permission.Write) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[OrganizationAddCollectionError]
      }
    }
  }

  "unShareItems" should {
    "should fail when itemService fails removing collectionsIds from shared" in new scope {
      val spyService = spy(service)
      doAnswer(_=>Success()).when(spyService).isAuthorized(any[ObjectId], any[Seq[ObjectId]], any[Permission])
      itemService.removeCollectionIdsFromShared(any[Seq[VersionedId[ObjectId]]], any[Seq[ObjectId]]) returns Failure(PlatformServiceError("test"))

      spyService.unShareItems(orgId, Seq(item.id), Seq(collection.id)) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[GeneralError]
      }

    }
  }

  "shareItems" should {
    "should fail when itemService cannot add collectionId to shared" in new scope {
      val spyService = spy(service)
      doAnswer(_=>Success()).when(spyService).isAuthorized(any[ObjectId], any[Seq[ObjectId]], any[Permission])
      itemService.findMultipleById(any[ObjectId]) returns Stream.Empty
      itemService.addCollectionIdToSharedCollections(any[Seq[VersionedId[ObjectId]]],any[ObjectId]) returns Failure(PlatformServiceError("test"))

      spyService.shareItems(orgId, Seq(item.id), collection.id) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[GeneralError]
      }
    }
  }

  "delete" should {
    "should fail when services throw SalatDAOUpdateError" in new scope {
      dao.removeById(collection.id) throws mock[SalatDAOUpdateError]

      service.delete(collection.id) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[GeneralError]
      }
    }

    "should fail when services throw SalatRemoveError" in new scope {
      dao.removeById(collection.id) throws mock[SalatRemoveError]

      service.delete(collection.id) match {
        case Success(value) => failure("Expected to fail with error")
        case Failure(error) => error must haveClass[GeneralError]
      }
    }
  }








}
