package org.corespring.services.salat

import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError
import org.corespring.services.item.ItemService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class OrgItemSharingServiceTest extends Specification with Mockito {

  trait scope extends Scope {
    val context = mock[Context]

    val orgCollectionService = {
      val m = mock[OrgCollectionService]
      m.isAuthorized(any[ObjectId], any[ObjectId], any[Permission]) returns true
      m
    }

    val itemService = mock[ItemService]
    val service = new OrgItemSharingService(itemService, orgCollectionService)

    val orgId = ObjectId.get
    val collection = ContentCollection("test-collection", orgId)
    val item = new Item(collectionId = collection.id.toString)
  }

  "shareItems" should {
    "should fail when orgCollectionService.isAuthorized is false" in new scope {
      orgCollectionService.isAuthorized(any[ObjectId], any[ObjectId], any[Permission]) returns false
      service.shareItems(orgId, Seq(item.id), collection.id) must_== Failure(_: PlatformServiceError)
    }
  }

  "unShareItems" should {
    "should fail when itemService fails removing collectionsIds from shared" in new scope {
      itemService.removeCollectionIdsFromShared(any[Seq[VersionedId[ObjectId]]], any[Seq[ObjectId]]) returns Failure(PlatformServiceError("test"))
      service.unShareItems(orgId, Seq(item.id), Seq(collection.id)) must_== Failure(_: PlatformServiceError)
    }
  }
}
