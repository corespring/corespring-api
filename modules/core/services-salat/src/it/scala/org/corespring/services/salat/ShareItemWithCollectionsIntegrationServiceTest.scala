package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.errors.{ ItemAuthorizationError, PlatformServiceError }
import org.corespring.errors.collection._
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.specification.After

import scalaz.{ Failure, Success, Validation }

class ShareItemWithCollectionsIntegrationServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends After with InsertionHelper {
    val service = services.shareItemWithCollectionsService

    val otherOrg = insertOrg("other-org")
    val rootOrg = insertOrg("root-org")

    val writableCollectionWithItem = insertCollection("writable-with-item", rootOrg)
    val writableCollection = insertCollection("writable", rootOrg)
    val readableCollection = insertCollection("readable", otherOrg)
    giveOrgAccess(rootOrg, readableCollection, Permission.Read)
    val otherOrgCollection = insertCollection("other-org-collection", otherOrg)

    val item = insertItem(writableCollectionWithItem.id)
    val otherItem = insertItem(otherOrgCollection.id)

    override def after: Any = removeAllData()

    def itemAuthorizationError(orgId: ObjectId, p: Permission, itemIds: VersionedId[ObjectId]*) = {
      Failure(ItemAuthorizationError(orgId, p, itemIds: _*))
    }
  }

  //TODO: lifted from ItemServiceTest - still relevant?
  //  "addCollectionIdToSharedCollections" should {
  //    trait insertCollectionIdToSharedCollections extends scope {
  //      val itemTwo = addItem(2, collectionOne)
  //      val sharedCollection = insertCollection("2", org)
  //    }
  //
  //    "add collectionId to item.sharedInCollections" in new insertCollectionIdToSharedCollections {
  //      service.addCollectionIdToSharedCollections(Seq(itemOne.id, itemTwo.id), sharedCollection.id)
  //      loadItem(itemOne.id).map(_.sharedInCollections.contains(sharedCollection.id))
  //      loadItem(itemTwo.id).map(_.sharedInCollections.contains(sharedCollection.id))
  //    }
  //
  //    "return ids of updated items when call was successful" in new insertCollectionIdToSharedCollections {
  //      val result = service.addCollectionIdToSharedCollections(Seq(itemOne.id, itemTwo.id), sharedCollection.id)
  //      result must_== Success(Seq(itemOne.id, itemTwo.id))
  //    }
  //  }
  //  "deleteFromSharedCollections" should {
  //    trait deleteFromSharedCollections extends scope {
  //      val collectionTwo = insertCollection("two", org)
  //      val sharedCollectionId = collectionTwo.id
  //      service.addCollectionIdToSharedCollections(Seq(itemOne.id), sharedCollectionId)
  //    }
  //
  //    "remove collection from one item" should {
  //      "return success" in new deleteFromSharedCollections() {
  //        service.deleteFromSharedCollections(sharedCollectionId) must_== Success()
  //      }
  //
  //      "update the item in db" in new deleteFromSharedCollections() {
  //        loadItem(itemOne.id).map(_.sharedInCollections === Seq(sharedCollectionId))
  //        service.deleteFromSharedCollections(sharedCollectionId)
  //        loadItem(itemOne.id).map(_.sharedInCollections === Seq.empty)
  //      }
  //    }
  //    "remove collection from multiples items" should {
  //      "update the items in db" in new deleteFromSharedCollections() {
  //        val itemTwo = addItem(2, collectionOne)
  //        service.addCollectionIdToSharedCollections(Seq(itemTwo.id), sharedCollectionId)
  //        loadItem(itemOne.id).map(_.sharedInCollections === Seq(sharedCollectionId))
  //        loadItem(itemTwo.id).map(_.sharedInCollections === Seq(sharedCollectionId))
  //        service.deleteFromSharedCollections(sharedCollectionId)
  //        loadItem(itemOne.id).map(_.sharedInCollections === Seq.empty)
  //        loadItem(itemTwo.id).map(_.sharedInCollections === Seq.empty)
  //      }
  //    }
  //  }

  "unShareItems" should {

    "remove shared item from collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id)
      service.unShareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
      service.isItemSharedWith(item.id, writableCollection.id) must_== false
    }

    "return error when org does not have write permissions for all collections" in new scope {
      service.unShareItems(rootOrg.id, Seq(item.id), readableCollection.id) must_== Failure(_: PlatformServiceError)
    }
  }

  "shareItems" should {

    "add the item to collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
      service.isItemSharedWith(item.id, writableCollection.id) must_== true
    }

    "return error when org cannot write into collection" in new scope {
      service.shareItems(rootOrg.id, Seq(item.id), readableCollection.id) must_== Failure(CantWriteToCollection(rootOrg.id, None, readableCollection.id))
    }

    "return error when org cannot write for all items" in new scope {
      service.shareItems(rootOrg.id, Seq(otherItem.id), writableCollection.id) must_== itemAuthorizationError(rootOrg.id, Permission.Read, otherItem.id)
    }
  }
}
