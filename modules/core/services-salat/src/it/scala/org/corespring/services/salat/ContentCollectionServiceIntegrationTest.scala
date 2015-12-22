package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.errors.ItemAuthorizationError
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ ContentCollection }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionUpdate
import org.specs2.mutable._

import scalaz.{ Failure, Success }

class ContentCollectionServiceIntegrationTest
  extends ServicesSalatIntegrationTest {

  "ContentCollectionService" should {

    trait scope extends After with InsertionHelper {
      val service = services.contentCollectionService

      val otherOrg = insertOrg("other-org")
      val rootOrg = insertOrg("root-org")
      val childOrg = insertOrg("child-org", Some(rootOrg.id))
      val publicOrg = insertOrg("public-org")

      //collections added to rootOrg
      val readableCollection = insertCollection("readable", otherOrg)
      //Give root org read access
      giveOrgAccess(rootOrg, readableCollection, Permission.Read)

      val writableCollection = insertCollection("writable-col", rootOrg)
      val writableCollectionWithItem = insertCollection("writable-with-item-col", rootOrg)
      val defaultCollection = insertCollection(ContentCollection.Default, rootOrg)
      val publicCollection = insertCollection("public-org-col", publicOrg, true)

      //rootOrg's writableCollectionWithItem contains one item
      val item = Item(
        collectionId = writableCollectionWithItem.id.toString,
        taskInfo = Some(TaskInfo(title = Some("title"))),
        standards = Seq("S1", "S2"))
      val itemId = services.itemService.insert(item).get

      override def after: Any = removeAllData()
    }

    "insertCollection" should {

      trait insertCollection extends scope {
        val newCollection = ContentCollection("child-org-col-2", childOrg.id, isPublic = false)
        service.insertCollection(newCollection)
      }

      "newCollection can be found by id" in new insertCollection {
        service.findOneById(newCollection.id) must_== Some(newCollection)
      }

      "collection's org has write permission to the collection" in new insertCollection {
        services.orgCollectionService.getPermission(childOrg.id, newCollection.id) must_== Some(Permission.Write)
      }

      "collection's org owns the collection" in new insertCollection {
        services.orgCollectionService.ownsCollection(childOrg, newCollection.id) must_== Success(true)
      }
    }

    "isPublic" should {

      "return true when collection is public" in new scope {
        service.isPublic(publicCollection.id) must_== true
      }

      "return false when collection is not public" in new scope {
        service.isPublic(readableCollection.id) must_== false
      }

      "return false when collection does not exist" in new scope {
        service.isPublic(ObjectId.get) must_== false
      }
    }

    "delete" should {

      trait delete extends scope

      "remove the collection from the collections" in new scope {
        val col = service.create("my-new-collection", rootOrg).toOption.get
        service.findOneById(col.id) !== None
        service.delete(col.id)
        service.findOneById(col.id) must_== None
      }

      "return an error if collection has items" in new scope {
        service.delete(writableCollectionWithItem.id).isFailure must_== true
      }

      "not remove the collection if it has items" in new scope {
        service.delete(writableCollectionWithItem.id).isFailure must_== true
        service.findOneById(writableCollectionWithItem.id) !== None
      }

      "remove the collection from all organizations" in new delete {
        val col = ContentCollection("test-col", rootOrg.id)
        service.insertCollection(col)
        services.orgCollectionService.getOrgsWithAccessTo(col.id).map(_.name) must_== Stream(rootOrg.name)
        val res = service.delete(col.id)
        services.orgCollectionService.getOrgsWithAccessTo(col.id) must_== Stream.empty
      }

      "remove the collection from shared collections" in new scope {
        val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
        val otherOrgItem = insertItem(otherOrgCollection.id)
        //allow root org read access to the item so that it can shared it
        giveOrgAccess(rootOrg, otherOrgCollection, Permission.Read)
        //now share it
        services.shareItemWithCollectionsService.shareItems(rootOrg.id, Seq(otherOrgItem.id), writableCollection.id)
        services.shareItemWithCollectionsService.isItemSharedWith(otherOrgItem.id, writableCollection.id) must_== true
        service.delete(writableCollection.id)
        services.shareItemWithCollectionsService.isItemSharedWith(otherOrgItem.id, writableCollection.id) must_== false
      }
    }

    "getPublicCollections" should {

      "return seq with public collection" in new scope {
        service.getPublicCollections must_== Seq(publicCollection)
      }
    }

    "create" should {

      "create a new collection" in new scope {
        val newCollection = service.create("my-new-collection", rootOrg).toOption
        service.findOneById(newCollection.get.id) must_== newCollection
      }
    }

    "update" should {

      "update name" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(Some("new-name"), None))
        service.findOneById(writableCollection.id) must_== Some(writableCollection.copy(name = "new-name"))
      }

      "update isPublic" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(None, Some(!writableCollection.isPublic)))
        service.findOneById(writableCollection.id) must_== Some(writableCollection.copy(isPublic = !writableCollection.isPublic))
      }

      "update name and isPublic" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(Some("new-name"), Some(!writableCollection.isPublic)))
        service.findOneById(writableCollection.id) must_==
          Some(writableCollection.copy(name = "new-name", isPublic = !writableCollection.isPublic))
      }
    }

  }
}
