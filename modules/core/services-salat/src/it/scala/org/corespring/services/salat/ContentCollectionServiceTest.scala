package org.corespring.services.salat

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ TaskInfo, Item }
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.specs2.mutable.{ BeforeAfter, Specification }
import org.specs2.mutable.{ After }

import scalaz.{ Failure, Success }

class ContentCollectionServiceTest
  extends ServicesSalatIntegrationTest {

  def calling(n: String) = s"when calling $n"

  "ContentCollectionService" should {

    trait xScope extends After {
      val service = services.contentCollectionService

      val rootOrg = services.orgService.insert(Organization("root-org"), None).toOption.get
      val childOrg = service.orgService.insert(Organization("child-org"), Some(rootOrg.id)).toOption.get
      val publicOrg = services.orgService.insert(Organization("public-org"), None).toOption.get

      val readableCollection = ContentCollection("readable-col", rootOrg.id)
      service.insertCollection(rootOrg.id, readableCollection, Permission.Read)

      val writableCollection = ContentCollection("writable-col", rootOrg.id)
      service.insertCollection(rootOrg.id, writableCollection, Permission.Write)

      val defaultCollection = ContentCollection("default", rootOrg.id)
      service.insertCollection(rootOrg.id, defaultCollection, Permission.Read)

      val noPermissionCollection = ContentCollection("no-permission-col", rootOrg.id)
      service.insertCollection(rootOrg.id, noPermissionCollection, Permission.None)

      val readableChildOrgCollection = ContentCollection("readable-child-org-col", childOrg.id)
      service.insertCollection(childOrg.id, readableChildOrgCollection, Permission.Read)

      val writableChildOrgCollection = ContentCollection("writable-child-org-col", childOrg.id)
      service.insertCollection(childOrg.id, writableChildOrgCollection, Permission.Write)

      val publicCollection = ContentCollection("public-org-col", publicOrg.id, isPublic = true)
      service.insertCollection(publicOrg.id, publicCollection, Permission.Write)

      val item = Item(
        collectionId = writableCollection.id.toString,
        taskInfo = Some(TaskInfo(title = Some("title"))),
        standards = Seq("S1", "S2"))
      val itemId = services.itemService.insert(item).get

      def findCollection(org: Organization, col: ContentCollection) = {
        service.getContentCollRefs(org.id, Permission.Read).find(
          _.collectionId == col.id)
      }

      override def after: Any = {
        services.itemService.purge(itemId)

        service.delete(readableCollection.id)
        service.delete(writableCollection.id)
        service.delete(defaultCollection.id)
        service.delete(noPermissionCollection.id)
        service.delete(readableChildOrgCollection.id)
        service.delete(writableChildOrgCollection.id)
        service.delete(publicCollection.id)

        services.orgService.delete(childOrg.id)
        services.orgService.delete(publicOrg.id)
        services.orgService.delete(rootOrg.id)
      }
    }

    calling("insertCollection") should {
      "work" in pending
      //TODO what does two phase commit mean?
      //TODO what should happen if organizationService fails to add a ref to the collection?
    }

    calling("addOrganizations") should {
      //TODO is commented out in the service, is it needed ?
      "work" in pending
    }

    calling("shareItemsMatchingQuery") should {
      "work" in pending
    }

    calling("shareItems") should {
      "work" in pending
    }

    calling("unShareItems") should {
      "work" in pending
    }

    calling("shareItemsMatchingQuery") should {
      "work" in pending
    }

    calling("getCollectionIds") should {
      "work" in pending
    }

    "listCollectionsByOrg" should {

      "list 3 collections for the childOrg" in new xScope {
        val cols = service.listCollectionsByOrg(childOrg.id).toSeq
        cols.length === 3
        cols.find(_.id == readableChildOrgCollection.id) !== None
        cols.find(_.id == writableChildOrgCollection.id) !== None
        cols.find(_.id == publicCollection.id) !== None
      }
    }

    "getPublicCollections" should {

      "return seq with public collection" in new xScope {
        service.getPublicCollections === Seq(publicCollection)
      }
    }

    "getContentCollRefs" should {

      "with Permission.Write" should {
        "should return CCRs for all nested orgs by default" in new xScope {
          var refs = service.getContentCollRefs(rootOrg.id, Permission.Write)
          refs.find(_.collectionId == writableCollection.id) !== None
          refs.find(_.collectionId == writableChildOrgCollection.id) !== None
          refs.length === 2
        }

        "should return CCRs for nested orgs when deep = true" in new xScope {
          var refs = service.getContentCollRefs(rootOrg.id, Permission.Write, deep = true)
          refs.find(_.collectionId == writableCollection.id) !== None
          refs.find(_.collectionId == writableChildOrgCollection.id) !== None
          refs.length === 2
        }

        "should return CCRs for a single org when deep = false" in new xScope {
          var refs = service.getContentCollRefs(rootOrg.id, Permission.Write, deep = false)
          refs.find(_.collectionId == writableCollection.id) !== None
          refs.length === 1
        }
      }

      "with permission = Read" should {

        "return all readable collections for rootOrg" in new xScope {
          var refs = service.getContentCollRefs(rootOrg.id, Permission.Read)

          refs.find(_.collectionId == writableCollection.id) !== None
          refs.find(_.collectionId == writableChildOrgCollection.id) !== None
          refs.find(_.collectionId == readableCollection.id) !== None
          refs.find(_.collectionId == readableChildOrgCollection.id) !== None
          refs.find(_.collectionId == defaultCollection.id) !== None
          refs.find(_.collectionId == publicCollection.id) !== None

          refs.length === 6
        }

        "return all readable collections for childOrg" in new xScope {
          var refs = service.getContentCollRefs(childOrg.id, Permission.Read)

          refs.find(_.collectionId == writableChildOrgCollection.id) !== None
          refs.find(_.collectionId == readableChildOrgCollection.id) !== None
          refs.find(_.collectionId == publicCollection.id) !== None

          refs.find(_.collectionId == writableCollection.id) === None
          refs.find(_.collectionId == readableCollection.id) === None
          refs.find(_.collectionId == defaultCollection.id) === None

          refs.length === 3
        }
      }

      //TODO Is Permission.None a valid scenario?
      "with permission none" should {
        "work" in pending
      }
    }

    "itemCount" should {

      "work" in new xScope {
        service.itemCount(writableCollection.id) === 1
      }

      "work" in new xScope {
        service.itemCount(readableCollection.id) === 0
      }
    }

    "getDefaultCollection" should {

      "return default collection" in new xScope {
        val collectionIds = Seq(readableCollection, writableCollection, defaultCollection).map(_.id)
        service.getDefaultCollection(collectionIds) === Some(defaultCollection)
      }

      "return None" in new xScope {
        val collectionIds = Seq(readableCollection, writableCollection).map(_.id)
        service.getDefaultCollection(collectionIds) === None
      }
    }

    "isPublic" should {

      "return true when collection is public" in new xScope {
        service.isPublic(publicCollection.id) === true
      }

      "return false when collection is not public" in new xScope {
        service.isPublic(readableCollection.id) === false
      }

      "return false when collection does not exist" in new xScope {
        service.isPublic(ObjectId.get) === false
      }
    }

    "isAuthorized" should {

      "work on a collection with read permission" in new xScope {
        service.isAuthorized(rootOrg.id, readableCollection.id, Permission.Read) === true
        service.isAuthorized(rootOrg.id, readableCollection.id, Permission.Write) === false
        service.isAuthorized(rootOrg.id, readableCollection.id, Permission.None) === true
      }

      "work on a collection with write permission" in new xScope {
        service.isAuthorized(rootOrg.id, writableCollection.id, Permission.Read) === true
        service.isAuthorized(rootOrg.id, writableCollection.id, Permission.Write) === true
        service.isAuthorized(rootOrg.id, writableCollection.id, Permission.None) === true
      }

      "work on a collection with no permission" in new xScope {
        service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.Read) === false
        service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.Write) === false
        service.isAuthorized(rootOrg.id, noPermissionCollection.id, Permission.None) === true
      }
    }


    "delete" should {

      "remove the collection from the collections" in new xScope {
        val col = service.create("my-new-collection", rootOrg).toOption.get
        service.findOneById(col.id) !== None
        service.delete(col.id)
        service.findOneById(col.id) === None
      }

      "return an error if collection has items" in new xScope {
        service.delete(writableCollection.id).isFailure === true
      }

      "not remove the collection if it has items" in new xScope {
        service.delete(writableCollection.id).isFailure === true
        service.findOneById(writableCollection.id) !== None
      }

      "remove the collection from all organizations" in new xScope {
        val col = ContentCollection("test-col", rootOrg.id)
        service.insertCollection(rootOrg.id, col, Permission.Read)
        service.insertCollection(childOrg.id, col, Permission.Read)
        val res = service.delete(col.id)
        findCollection(childOrg, col) === None
        findCollection(rootOrg, col) === None

      }

      "remove the collection from shared collections" in pending

      //TODO difficult to throw an error
      "return an error when service calls return SalatRemoveError" in pending

      //TODO difficult to throw an error
      "return an error when service calls return SalatDAOUpdateError" in pending

      //TODO roll back not implemented in service
      "roll back when organization could not be updated" in pending

      //TODO roll back not implemented in service
      "roll back when items could not be updated" in pending
    }

    "create" should {

      "create a new collection" in new xScope {
        val col = service.create("my-new-collection", rootOrg).toOption.get
        service.getCollections(rootOrg.id, Permission.Write).isSuccess === true
        service.delete(col.id)
      }
    }

  }
}
