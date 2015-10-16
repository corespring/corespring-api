package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionUpdate
import org.corespring.services.errors._
import org.specs2.mutable._
import org.specs2.specification.Scope

import scalaz.{ Validation, Failure, Success }

class ContentCollectionServiceIntegrationTest
  extends ServicesSalatIntegrationTest {

  "ContentCollectionService" should {

    trait scope extends After {
      val service = services.contentCollectionService

      val rootOrg = services.orgService.insert(Organization("root-org"), None).toOption.get
      val childOrg = services.orgService.insert(Organization("child-org"), Some(rootOrg.id)).toOption.get
      val publicOrg = services.orgService.insert(Organization("public-org"), None).toOption.get

      //collections added to rootOrg
      val readableCollection = ContentCollection("readable-col", rootOrg.id)
      service.insertCollection(rootOrg.id, readableCollection, Permission.Read)

      val writableCollection = ContentCollection("writable-col", rootOrg.id)
      service.insertCollection(rootOrg.id, writableCollection, Permission.Write)

      val writableCollectionWithItem = ContentCollection("writable-with-item-col", rootOrg.id)
      service.insertCollection(rootOrg.id, writableCollectionWithItem, Permission.Write)

      val defaultCollection = ContentCollection("default", rootOrg.id)
      service.insertCollection(rootOrg.id, defaultCollection, Permission.Read)

      val noPermissionCollection = ContentCollection("no-permission-col", rootOrg.id)
      service.insertCollection(rootOrg.id, noPermissionCollection, Permission.None)

      //collections added to childOrg
      val readableChildOrgCollection = ContentCollection("readable-child-org-col", childOrg.id)
      service.insertCollection(childOrg.id, readableChildOrgCollection, Permission.Read)

      val writableChildOrgCollection = ContentCollection("writable-child-org-col", childOrg.id)
      service.insertCollection(childOrg.id, writableChildOrgCollection, Permission.Write)

      //collection added to publicOrg
      val publicCollection = ContentCollection("public-org-col", publicOrg.id, isPublic = true)
      service.insertCollection(publicOrg.id, publicCollection, Permission.Write)

      //rootOrg's writableCollectionWithItem contains one item
      val item = Item(
        collectionId = writableCollectionWithItem.id.toString,
        taskInfo = Some(TaskInfo(title = Some("title"))),
        standards = Seq("S1", "S2"))
      val itemId = services.itemService.insert(item).get

      override def after: Any = removeAllData()

      def authorizationError[R](p: Permission, colls: ContentCollection*): Validation[CollectionAuthorizationError, R] = {
        Failure(CollectionAuthorizationError(rootOrg.id, p, colls.map(_.id): _*))
      }

      def itemAuthorizationError(orgId: ObjectId, p: Permission, itemIds: VersionedId[ObjectId]*) = {
        Failure(ItemAuthorizationError(orgId, p, itemIds: _*))
      }
    }

    "ownsCollection" should {
      "should return Success when org owns collection" in new scope {
        service.ownsCollection(rootOrg, writableCollection.id).isSuccess must_== true
      }

      "should return Failure when org does not own collection" in new scope {
        service.ownsCollection(childOrg, writableCollection.id).isFailure must_== true
      }

      "should return Failure when collection does not exist" in new scope {
        service.ownsCollection(childOrg, ObjectId.get).isFailure must_== true
      }
    }

    "shareCollectionWithOrg" should {
      "share the collection with the org" in new scope {
        service.isAuthorized(childOrg.id, writableCollection.id, Permission.Read) must_== Failure(_: PlatformServiceError)
        service.shareCollectionWithOrg(writableCollection.id, childOrg.id, Permission.Read)
        service.isAuthorized(childOrg.id, writableCollection.id, Permission.Read) must_== Success()
      }
    }

    "insertCollection" should {

      trait insertCollection extends scope {

        val newCollection = ContentCollection("child-org-col-2", childOrg.id, isPublic = false)

        override def after: Any = {
          service.delete(newCollection.id)
          super.after
        }

        def isEnabled() = {
          service.getContentCollRefs(childOrg.id, Permission.Write, deep = false)
            .find(_.collectionId == newCollection.id).map(_.enabled).getOrElse(false)
        }

        def getPermissions() = {
          service.getContentCollRefs(childOrg.id, Permission.Read, deep = false)
            .find(_.collectionId == newCollection.id).map(_.pval).getOrElse(false)
        }
      }

      "insert newCollection as enabled by default" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write)
        isEnabled() must_== true
      }

      "be able to insert newCollection as enabled" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write, enabled = true)
        isEnabled() must_== true
      }

      "be able to insert newCollection as disabled" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write, enabled = false)
        isEnabled() must_== false
      }

      "be able to insert newCollection as writable" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write)
        getPermissions() must_== Permission.Write.value
      }

      "be able to insert newCollection as readable" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Read)
        getPermissions() must_== Permission.Read.value
      }
    }

    "unShareItems" should {

      "remove shared item from collection" in new scope {
        service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id)
        service.unShareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
        service.isItemSharedWith(item.id, writableCollection.id) must_== false
      }

      "return error when org does not have write permissions for all collections" in new scope {
        service.unShareItems(rootOrg.id, Seq(item.id), Seq(readableCollection.id)) must_== Failure(_: CollectionAuthorizationError)
      }
    }

    "shareItems" should {

      "add the item to collection" in new scope {
        service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id) must_== Success(Seq(item.id))
        service.isItemSharedWith(item.id, writableCollection.id) must_== true
      }

      "return error when org cannot write into collection" in new scope {
        service.shareItems(rootOrg.id, Seq(item.id), readableCollection.id) must_== authorizationError(Permission.Write, readableCollection)
      }

      "return error when org cannot read all items" in new scope {
        service.shareItems(childOrg.id, Seq(item.id), writableChildOrgCollection.id) must_== itemAuthorizationError(childOrg.id, Permission.Read, item.id)
      }
    }

    "getCollectionIds" should {

      trait getCollectionIds extends scope

      "with Permission.Write" should {
        "should return CCRs for all nested orgs by default" in new getCollectionIds {
          service.getCollectionIds(rootOrg.id, Permission.Write) must_== Seq(
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection).map(_.id)
        }

        "should return CCRs for nested orgs when deep = true" in new getCollectionIds {
          service.getCollectionIds(rootOrg.id, Permission.Write, deep = true) must_== Seq(
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection).map(_.id)
        }

        "should return CCRs for a single org when deep = false" in new getCollectionIds {
          service.getCollectionIds(rootOrg.id, Permission.Write, deep = false) must_== Seq(
            writableCollection,
            writableCollectionWithItem).map(_.id)
        }
      }

      "with permission = Read" should {

        "return all readable collections for rootOrg" in new getCollectionIds {
          service.getCollectionIds(rootOrg.id, Permission.Read).sorted must_== Seq(
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection,
            readableCollection,
            readableChildOrgCollection,
            defaultCollection,
            publicCollection).map(_.id).sorted
        }

        "return all readable collections for childOrg" in new getCollectionIds {
          service.getCollectionIds(childOrg.id, Permission.Read).sorted must_== Seq(
            writableChildOrgCollection,
            readableChildOrgCollection,
            publicCollection).map(_.id).sorted
        }
      }
    }

    "listAllCollectionsAvailableForOrg" should {

      trait listAllCollectionsAvailableForOrg extends Scope with After {
        val service = services.contentCollectionService
        val orgOne = services.orgService.insert(Organization(id = ObjectId.get, name = "org-one"), None).toOption.get
        val writeOne = ContentCollection("write-one", orgOne.id, false)
        val readOne = ContentCollection("read-one", orgOne.id, false)
        val publicOne = ContentCollection("public", orgOne.id, true)
        service.insertCollection(orgOne.id, writeOne, Permission.Write, true)
        service.insertCollection(orgOne.id, readOne, Permission.Read, true)
        service.insertCollection(orgOne.id, publicOne, Permission.Read, true)

        override def after: Any = removeAllData()
      }

      "list all the collections for org" in new listAllCollectionsAvailableForOrg {
        lazy val result = service.listAllCollectionsAvailableForOrg(orgOne.id).toSeq
        result must_== Stream(
          CollectionInfo(writeOne, 0, orgOne.id, Permission.Write),
          CollectionInfo(readOne, 0, orgOne.id, Permission.Read),
          CollectionInfo(publicOne, 0, orgOne.id, Permission.Read))
      }

      "list public collections for other org" in new listAllCollectionsAvailableForOrg {
        val orgTwo = services.orgService.insert(Organization(id = ObjectId.get, name = "org-two"), None).toOption.get
        lazy val result = service.listAllCollectionsAvailableForOrg(orgTwo.id)
        result must_== Stream(
          CollectionInfo(publicOne, 0, orgTwo.id, Permission.Read))
      }
    }

    "listCollectionsByOrg" should {

      trait listCollectionsByOrg extends scope

      "list 3 collections for the childOrg" in new listCollectionsByOrg {
        service.listCollectionsByOrg(childOrg.id) === Stream(
          readableChildOrgCollection,
          writableChildOrgCollection,
          publicCollection)
      }
    }

    "getPublicCollections" should {

      "return seq with public collection" in new scope {
        service.getPublicCollections must_== Seq(publicCollection)
      }
    }

    "getContentCollRefs" should {

      trait getContentCollRefs extends scope

      "with Permission.Write" should {
        "should return CCRs for all nested orgs by default" in new getContentCollRefs {
          val expectedIds = Seq(writableCollection, writableCollectionWithItem, writableChildOrgCollection).map(_.id)
          service.getContentCollRefs(rootOrg.id, Permission.Write).map(_.collectionId) must_== expectedIds
        }

        "should return CCRs for nested orgs when deep = true" in new getContentCollRefs {
          val expectedIds = Seq(writableCollection, writableCollectionWithItem, writableChildOrgCollection).map(_.id)
          service.getContentCollRefs(rootOrg.id, Permission.Write, deep = true).map(_.collectionId) == expectedIds
        }

        "should return CCRs for a single org when deep = false" in new getContentCollRefs {
          val expectedIds = Seq(writableCollection, writableCollectionWithItem).map(_.id)
          service.getContentCollRefs(rootOrg.id, Permission.Write, deep = false).map(_.collectionId) must_== expectedIds
        }
      }

      "with permission = Read" should {

        "return all readable collections for rootOrg" in new getContentCollRefs {
          val expectedIds = Seq(writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection,
            readableCollection,
            readableChildOrgCollection,
            defaultCollection,
            publicCollection).map(_.id).sorted
          service.getContentCollRefs(rootOrg.id, Permission.Read).map(_.collectionId).sorted must_== expectedIds
        }

        "return all readable collections for childOrg" in new getContentCollRefs {
          val expectedIds = Seq(
            writableChildOrgCollection,
            readableChildOrgCollection,
            publicCollection).map(_.id).sorted
          service.getContentCollRefs(childOrg.id, Permission.Read).map(_.collectionId).sorted must_== expectedIds
        }
      }
    }

    "itemCount" should {

      "return 1 for collection with 1 item" in new scope {
        service.itemCount(writableCollectionWithItem.id) must_== 1
      }

      "return 0 for collection with no items" in new scope {
        service.itemCount(readableCollection.id) must_== 0
      }
    }

    "getDefaultCollection" should {

      "return default collection" in new scope {
        val collectionIds = Seq(readableCollection, writableCollection, defaultCollection).map(_.id)
        service.getDefaultCollection(collectionIds) must_== Some(defaultCollection)
      }

      "return None" in new scope {
        val collectionIds = Seq(readableCollection, writableCollection).map(_.id)
        service.getDefaultCollection(collectionIds) must_== None
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

    "isAuthorized" should {

      trait isAuthorized extends scope {
        def p: Permission
        def auth(collections: ContentCollection*) = service.isAuthorized(rootOrg.id, collections.map(_.id), p)

        def authorizationError[R](collections: ContentCollection*): Validation[CollectionAuthorizationError, R] = authorizationError(p, collections: _*)
      }

      "with read permission" should {

        trait isReadAuthorized extends isAuthorized {
          override def p = Permission.Read
        }

        "return success when collection is readable" in new isReadAuthorized {
          auth(readableCollection) must_== Success()
        }

        "return success when collection is writable" in new isReadAuthorized {
          auth(writableCollection) must_== Success()
        }

        "return failure when collection has permission none" in new isReadAuthorized {
          auth(noPermissionCollection) must_== authorizationError(noPermissionCollection)
        }

        "return failure when one collection is not readable" in new isReadAuthorized {
          auth(readableCollection, noPermissionCollection) must_== authorizationError(noPermissionCollection)
        }

        "return success when all collections are readable" in new isReadAuthorized {
          auth(readableCollection, writableCollection) must_== Success()
        }
      }

      "with write permission" should {

        trait isWriteAuthorized extends isAuthorized {
          override def p = Permission.Write
        }

        "return failure when collection is readable" in new isWriteAuthorized {
          auth(readableCollection) must_== authorizationError(readableCollection)
        }

        "return success when collection is writable" in new isWriteAuthorized {
          auth(writableCollection) must_== Success()
        }

        "return failure when collection has permission none" in new isWriteAuthorized {
          auth(noPermissionCollection) must_== authorizationError(noPermissionCollection)
        }

        "return failure when one collection is not writable" in new isWriteAuthorized {
          auth(readableCollection, writableCollection) must_== authorizationError(readableCollection)
        }

        "return success when all collections are writable" in new isWriteAuthorized {
          auth(writableCollectionWithItem, writableCollection) must_== Success()
        }
      }

      /**
       * TODO: Permission logic clean up.
       * below we ask does org x have no permission for collection y? and we get a Success() back
       * which means 'yes org x doesn't have permission for collection y'
       * this is a question that can be asked asking if org x has permission read/write for collection y.
       * So really Permission.None should be removed in favour of Option[Permission]
       */
      "with none permission" should {

        trait isNoneAuthorized extends isAuthorized {
          override def p = Permission.None
        }

        "return failure when collection is readable" in new isNoneAuthorized {
          auth(readableCollection) must_== Success()
        }

        "return failure when collection is writable" in new isNoneAuthorized {
          auth(writableCollection) must_== Success()
        }

        "return failure when collection has permission none" in new isNoneAuthorized {
          auth(noPermissionCollection) must_== Success()
        }
      }
    }

    "delete" should {

      trait delete extends scope {
        def assertCollectionHasBeenRemoved(org: Organization, col: ContentCollection) = {
          service.getContentCollRefs(org.id, Permission.Read).find(
            _.collectionId.equals(col.id)) match {
              case None =>
              case _ => failure(s"Collection has not been removed: ${col.name} ${col.id}")
            }
        }
      }

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
        service.insertCollection(rootOrg.id, col, Permission.Read)
        service.insertCollection(childOrg.id, col, Permission.Read)
        val res = service.delete(col.id)
        assertCollectionHasBeenRemoved(childOrg, col)
        assertCollectionHasBeenRemoved(rootOrg, col)
      }

      "remove the collection from shared collections" in new scope {
        def addCollectionToSharedCollectionsOfItem() = {
          services.itemService.addCollectionIdToSharedCollections(Seq(item.id), writableCollection.id)
        }

        def isCollectionInSharedCollections(): Boolean = {
          services.itemService.findOneById(item.id) match {
            case Some(itm) => itm.sharedInCollections.contains(writableCollection.id)
            case None => {
              failure("Item not found")
              false
            }
          }
        }

        addCollectionToSharedCollectionsOfItem()
        isCollectionInSharedCollections() must_== true
        service.delete(writableCollection.id)
        isCollectionInSharedCollections() must_== false
      }

      //TODO roll back not implemented in service
      "roll back when organization could not be updated" in pending

      //TODO roll back not implemented in service
      "roll back when items could not be updated" in pending
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
