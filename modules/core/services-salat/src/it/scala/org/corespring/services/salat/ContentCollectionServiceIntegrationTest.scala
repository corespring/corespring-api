package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.ContentCollectionUpdate
import org.corespring.services.errors._
import org.specs2.mutable._
import org.specs2.specification.Scope

import scalaz.{ Failure, Success }

class ContentCollectionServiceIntegrationTest
  extends ServicesSalatIntegrationTest {

  def calling(n: String) = s"when calling $n"

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

      override def after: Any = clearDb()
    }

    "ownsCollection" should {
      "should return Success when org owns collection" in new scope {
        service.ownsCollection(rootOrg, writableCollection.id).isSuccess === true
      }

      "should return Failure when org does not own collection" in new scope {
        service.ownsCollection(childOrg, writableCollection.id).isFailure === true
      }

      "should return Failure when collection does not exist" in new scope {
        service.ownsCollection(childOrg, ObjectId.get).isFailure === true
      }
    }

    "shareCollectionWithOrg" should {
      "should work" in new scope {
        service.getCollectionIds(childOrg.id, Permission.Read).contains(writableCollection.id) === false
        service.shareCollectionWithOrg(writableCollection.id, childOrg.id, Permission.Read)
        service.getCollectionIds(childOrg.id, Permission.Read).contains(writableCollection.id) === true
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
        isEnabled() === true
      }

      "be able to insert newCollection as enabled" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write, enabled = true)
        isEnabled() === true
      }

      "be able to insert newCollection as disabled" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write, enabled = false)
        isEnabled() === false
      }

      "be able to insert newCollection as writable" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Write)
        getPermissions() === Permission.Write.value
      }

      "be able to insert newCollection as readable" in new insertCollection {
        service.insertCollection(childOrg.id, newCollection, Permission.Read)
        getPermissions() === Permission.Read.value
      }
    }

    "unShareItems" should {

      "remove shared item from collection" in new scope {
        service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id)
        val res = service.unShareItems(rootOrg.id, Seq(item.id), writableCollection.id)
        res match {
          case Success(items) => service.isItemSharedWith(itemId, writableCollection.id) === false
          case Failure(error) => failure(s"Unexpected failure: $error")
        }
      }

      "return error when org does not have write permissions for all collections" in new scope {
        val res = service.unShareItems(rootOrg.id, Seq(item.id), Seq(readableCollection.id))
        res match {
          case Success(x) => failure("Expected to fail with error")
          case Failure(y) => y must haveClass[CollectionAuthorizationError]
        }
      }
    }

    "shareItems" should {

      "add the item to collection" in new scope {
        val res = service.shareItems(rootOrg.id, Seq(item.id), writableCollection.id)
        res match {
          case Success(items) => service.isItemSharedWith(items(0), writableCollection.id) === true
          case Failure(error) => failure(s"Unexpected failure: $error")
        }
      }

      "return error when org cannot write into collection" in new scope {
        val res = service.shareItems(rootOrg.id, Seq(item.id), readableCollection.id)
        res match {
          case Success(x) => failure("Expected to fail with error")
          case Failure(y) => y must haveClass[CollectionAuthorizationError]
        }
      }

      "return error when org cannot read all items" in new scope {
        val res = service.shareItems(childOrg.id, Seq(item.id), writableChildOrgCollection.id)
        res match {
          case Success(x) => failure("Expected to fail with error")
          case Failure(y) => y must haveClass[ItemAuthorizationError]
        }
      }
    }

    "getCollectionIds" should {

      trait getCollectionIds extends scope {
        def assertResult(ids: Seq[ObjectId], cols: ContentCollection*) = {
          cols.map { col =>
            ids.find(_.equals(col.id)) match {
              case None => failure(s"CollectionId not found: ${col.name} ${col.id}")
              case _ =>
            }
          }
          if (ids.length != cols.length) {
            failure(s"unexpected difference in length: ids: ${ids.length} cols: ${cols.length}")
          }
        }
      }

      "with Permission.Write" should {
        "should return CCRs for all nested orgs by default" in new getCollectionIds {
          var res = service.getCollectionIds(rootOrg.id, Permission.Write)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection)
        }

        "should return CCRs for nested orgs when deep = true" in new getCollectionIds {
          var res = service.getCollectionIds(rootOrg.id, Permission.Write, deep = true)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection)
        }

        "should return CCRs for a single org when deep = false" in new getCollectionIds {
          var res = service.getCollectionIds(rootOrg.id, Permission.Write, deep = false)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem)
        }
      }

      "with permission = Read" should {

        "return all readable collections for rootOrg" in new getCollectionIds {
          var res = service.getCollectionIds(rootOrg.id, Permission.Read)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection,
            readableCollection,
            readableChildOrgCollection,
            defaultCollection,
            publicCollection)
        }

        "return all readable collections for childOrg" in new getCollectionIds {
          var res = service.getCollectionIds(childOrg.id, Permission.Read)
          assertResult(res,
            writableChildOrgCollection,
            readableChildOrgCollection,
            publicCollection)
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

        override def after: Any = clearDb()
      }

      "list all the collections for org" in new listAllCollectionsAvailableForOrg {
        lazy val result = service.listAllCollectionsAvailableForOrg(orgOne.id).toSeq
        result === Stream(
          CollectionInfo(writeOne, 0, orgOne.id, Permission.Write),
          CollectionInfo(readOne, 0, orgOne.id, Permission.Read),
          CollectionInfo(publicOne, 0, orgOne.id, Permission.Read))
      }

      "list public collections for other org" in new listAllCollectionsAvailableForOrg {
        val orgTwo = services.orgService.insert(Organization(id = ObjectId.get, name = "org-two"), None).toOption.get
        lazy val result = service.listAllCollectionsAvailableForOrg(orgTwo.id)
        result === Stream(
          CollectionInfo(publicOne, 0, orgTwo.id, Permission.Read))
      }
    }

    "listCollectionsByOrg" should {

      trait listCollectionsByOrg extends scope {
        def assertResult(res: Seq[ContentCollection], cols: ContentCollection*) = {
          cols.map { col =>
            res.find(_.id.equals(col.id)) match {
              case None => failure(s"CollectionId not found: ${col.name} ${col.id}")
              case _ =>
            }
          }
          if (res.length != cols.length) {
            failure(s"unexpected difference in length: res: ${res.length} cols: ${cols.length}")
          }
        }
      }

      "list 3 collections for the childOrg" in new listCollectionsByOrg {
        val cols = service.listCollectionsByOrg(childOrg.id).toSeq
        assertResult(cols,
          readableChildOrgCollection,
          writableChildOrgCollection,
          publicCollection)
      }
    }

    "getPublicCollections" should {

      "return seq with public collection" in new scope {
        service.getPublicCollections match {
          case Nil => failure("Should have found a public collection")
          case seq => seq.length === 1 && seq(0).id === publicCollection.id
        }
      }
    }

    "getContentCollRefs" should {

      trait getContentCollRefs extends scope {
        def assertResult(refs: Seq[ContentCollRef], cols: ContentCollection*) = {
          cols.map { col =>
            refs.find(_.collectionId.equals(col.id)) match {
              case None => failure(s"Collection not found: ${col.name} ${col.id}")
              case _ =>
            }
          }
          if (refs.length != cols.length) {
            failure(s"unexpected difference in length: refs: ${refs.length} cols: ${cols.length}")
          }
        }
      }

      "with Permission.Write" should {
        "should return CCRs for all nested orgs by default" in new getContentCollRefs {
          var res = service.getContentCollRefs(rootOrg.id, Permission.Write)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection)
        }

        "should return CCRs for nested orgs when deep = true" in new getContentCollRefs {
          var res = service.getContentCollRefs(rootOrg.id, Permission.Write, deep = true)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection)
        }

        "should return CCRs for a single org when deep = false" in new getContentCollRefs {
          var res = service.getContentCollRefs(rootOrg.id, Permission.Write, deep = false)
          assertResult(res, writableCollection, writableCollectionWithItem)
        }
      }

      "with permission = Read" should {

        "return all readable collections for rootOrg" in new getContentCollRefs {
          var res = service.getContentCollRefs(rootOrg.id, Permission.Read)
          assertResult(res,
            writableCollection,
            writableCollectionWithItem,
            writableChildOrgCollection,
            readableCollection,
            readableChildOrgCollection,
            defaultCollection,
            publicCollection)
        }

        "return all readable collections for childOrg" in new getContentCollRefs {
          var res = service.getContentCollRefs(childOrg.id, Permission.Read)
          assertResult(res,
            writableChildOrgCollection,
            readableChildOrgCollection,
            publicCollection)
        }
      }
    }

    "itemCount" should {

      "work" in new scope {
        service.itemCount(writableCollectionWithItem.id) === 1
      }

      "work" in new scope {
        service.itemCount(readableCollection.id) === 0
      }
    }

    "getDefaultCollection" should {

      "return default collection" in new scope {
        val collectionIds = Seq(readableCollection, writableCollection, defaultCollection).map(_.id)
        service.getDefaultCollection(collectionIds) === Some(defaultCollection)
      }

      "return None" in new scope {
        val collectionIds = Seq(readableCollection, writableCollection).map(_.id)
        service.getDefaultCollection(collectionIds) === None
      }
    }

    "isPublic" should {

      "return true when collection is public" in new scope {
        service.isPublic(publicCollection.id) === true
      }

      "return false when collection is not public" in new scope {
        service.isPublic(readableCollection.id) === false
      }

      "return false when collection does not exist" in new scope {
        service.isPublic(ObjectId.get) === false
      }
    }

    "isAuthorized" should {

      "with read permission" should {
        "return success when collection is readable" in new scope {
          service.isAuthorized(rootOrg.id, readableCollection.id, Permission.Read).isSuccess === true
        }
        "return success when collection is writable" in new scope {
          service.isAuthorized(rootOrg.id, writableCollection.id, Permission.Read).isSuccess === true
        }
        "return failure when collection has permission none" in new scope {
          service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.Read).isFailure === true
        }
        "return failure when one collection is not readable" in new scope {
          service.isAuthorized(rootOrg.id, Seq(readableCollection.id, noPermissionCollection.id), Permission.Read).isFailure === true
        }
        "return success when all collections are readable" in new scope {
          service.isAuthorized(rootOrg.id, Seq(readableCollection.id, writableCollection.id), Permission.Read).isSuccess === true
        }
      }

      "with write permission" should {
        "return failure when collection is readable" in new scope {
          service.isAuthorized(rootOrg.id, readableCollection.id, Permission.Write).isFailure === true
        }
        "return success when collection is writable" in new scope {
          service.isAuthorized(rootOrg.id, writableCollection.id, Permission.Write).isSuccess === true
        }
        "return failure when collection has permission none" in new scope {
          service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.Write).isFailure === true
        }
        "return failure when one collection is not writable" in new scope {
          service.isAuthorized(rootOrg.id, Seq(readableCollection.id, writableCollection.id), Permission.Write).isFailure === true
        }
        "return success when all collections are writable" in new scope {
          service.isAuthorized(rootOrg.id, Seq(writableCollectionWithItem.id, writableCollection.id), Permission.Write).isSuccess === true
        }
      }

      "with none permission" should {
        "return failure when collection is readable" in new scope {
          service.isAuthorized(rootOrg.id, readableCollection.id, Permission.None).isSuccess === true
        }
        "return failure when collection is writable" in new scope {
          service.isAuthorized(rootOrg.id, writableCollection.id, Permission.None).isSuccess === true
        }
        "return failure when collection has permission none" in new scope {
          service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.None).isSuccess === true
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
        service.findOneById(col.id) === None
      }

      "return an error if collection has items" in new scope {
        service.delete(writableCollectionWithItem.id).isFailure === true
      }

      "not remove the collection if it has items" in new scope {
        service.delete(writableCollectionWithItem.id).isFailure === true
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
        isCollectionInSharedCollections() === true
        service.delete(writableCollection.id)
        isCollectionInSharedCollections() === false
      }

      //TODO roll back not implemented in service
      "roll back when organization could not be updated" in pending

      //TODO roll back not implemented in service
      "roll back when items could not be updated" in pending
    }

    "create" should {

      "create a new collection" in new scope {
        val col = service.create("my-new-collection", rootOrg).toOption.get
        service.getCollections(rootOrg.id, Permission.Write).isSuccess === true
        service.delete(col.id)
      }
    }

    "update" should {

      "update name" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(Some("new-name"), None))
        service.findOneById(writableCollection.id).get.name === "new-name"
        service.findOneById(writableCollection.id).get.isPublic === writableCollection.isPublic
      }

      "update isPublic" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(None, Some(!writableCollection.isPublic)))
        service.findOneById(writableCollection.id).get.name === writableCollection.name
        service.findOneById(writableCollection.id).get.isPublic === !writableCollection.isPublic
      }

      "update name and isPublic" in new scope {
        service.update(writableCollection.id, ContentCollectionUpdate(Some("new-name"), Some(!writableCollection.isPublic)))
        service.findOneById(writableCollection.id).get.name === "new-name"
        service.findOneById(writableCollection.id).get.isPublic === !writableCollection.isPublic
      }
    }

  }
}
