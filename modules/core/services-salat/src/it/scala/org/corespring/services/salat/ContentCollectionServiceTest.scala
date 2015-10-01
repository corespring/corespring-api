package org.corespring.services.salat

import com.mongodb.DBObject
import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.specs2.mutable.{ BeforeAfter, Specification }
import org.specs2.specification.{ After, Scope }

import scalaz.{ Failure, Success }

class ContentCollectionServiceTest
  extends ServicesSalatIntegrationTest {

  def calling(n: String) = s"when calling $n"

  "ContentCollectionService" should {

    trait baseScope extends After {
      val service = services.contentCollectionService
      val org = services.orgService.insert(Organization("test-org"), None).toOption.get

      override def after: Any = {
        services.orgService.delete(org.id)
      }
    }

    trait withSpies {

      var serviceSpy = spy(services.contentCollectionService)

      val orgServiceSpy = spy(serviceSpy.orgService)
      serviceSpy.orgService returns orgServiceSpy

      val itemServiceSpy = spy(serviceSpy.itemService)
      serviceSpy.itemService returns itemServiceSpy
    }

    class withCollection(
      isPublic: Boolean = false,
      permission: Permission = Permission.Read) extends baseScope {
      val collection = ContentCollection("test-col", org.id, isPublic)
      service.insertCollection(org.id, collection, permission)

      override def after: Any = {
        service.delete(collection.id)
        super.after
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

    "getContentCollRefs" should {
      class scope extends withCollection with withSpies

      "search deeply for orgs by default" in new scope {
        serviceSpy.getContentCollRefs(org.id, Permission.Read)
        there was one(orgServiceSpy).orgsWithPath(org.id, true)
      }
      "pass deep to orgService" in new scope {
        serviceSpy.getContentCollRefs(org.id, Permission.Read, deep=false)
        there was one(orgServiceSpy).orgsWithPath(org.id, false)
      }

      "with permission = Read" should {
        "return readable collections only" in pending
        "add public collections" in pending

      }
      "with permission = Write" should {
        "return writable collections only" in pending
        "not add public collections" in pending
      }
      "with permission = None" should {
        //TODO Is Permission = None a valid scenario?
        "work" in pending
      }
    }


    "getPublicCollections" should {

      "return empty seq" in new withCollection(isPublic=false) {
        service.getPublicCollections === Seq.empty
      }

      "return seq with collection" in new withCollection(isPublic=true) {
        service.getPublicCollections === Seq(collection)
      }
    }

    "itemCount" should {

      class scope extends withCollection with withSpies

      "delegate to itemService.count" in new scope {
        serviceSpy.itemCount(collection.id)
        there was one(itemServiceSpy).count(
          MongoDBObject("collectionId" -> collection.id.toString), None)
      }
    }

    "getDefaultCollection" should {

      class withCollections(collectionNames: String*) extends baseScope {

        val collections = collectionNames.map(name => {
          val collection = ContentCollection(name, org.id)
          service.insertCollection(org.id, collection, Permission.Read)
          collection
        })
        val collectionIds = collections.map(_.id)

        override def after: Any = {
          for (id <- collectionIds) {
            service.delete(id)
          }
          super.after
        }
      }

      "return default collection" in new withCollections("a", "b", "c", "default") {
        service.getDefaultCollection(collectionIds) === Some(collections(3))
      }

      "return None" in new withCollections("a", "b", "c") {
        service.getDefaultCollection(collectionIds) === None
      }
    }

    "isPublic" should {

      class withIsPublic(isPublic: Boolean) extends withCollection(isPublic)

      "return true when collection is public" in new withIsPublic(true) {
        service.isPublic(collection.id) === true
      }

      "return false when collection is not public" in new withIsPublic(false) {
        service.isPublic(collection.id) === false
      }
    }

    "isAuthorized" should {

      class withPermission(p: Permission) extends withCollection(isPublic=false, p) {

        def canRead() = {
          service.isAuthorized(org.id, collection.id, Permission.Read)
        }

        def canWrite() = {
          service.isAuthorized(org.id, collection.id, Permission.Write)
        }

        def canNone() = {
          service.isAuthorized(org.id, collection.id, Permission.None)
        }
      }

      "work on a collection with read permission" in new withPermission(Permission.Read) {
        canRead() === true
        canWrite() === false
        canNone() === true
      }

      "work on a collection with write permission" in new withPermission(Permission.Write) {
        canRead() === true
        canWrite() === true
        canNone() === true
      }

      "work on a collection with none permission" in new withPermission(Permission.None) {
        canRead() === false
        canWrite() === false
        canNone() === true
      }
    }

    "delete" should {

      class scope extends withCollection with withSpies

      "remove the collection from the collections" in new scope() {
        service.findOneById(collection.id) !== None
        service.delete(collection.id)
        service.findOneById(collection.id) === None
      }

      "remove the collection from all organizations" in new scope() {
        serviceSpy.delete(collection.id)
        there was one(orgServiceSpy).deleteCollectionFromAllOrganizations(collection.id)
      }

      "remove the collection from shared collections" in new scope() {
        serviceSpy.delete(collection.id)
        there was one(itemServiceSpy).deleteFromSharedCollections(collection.id)
      }

      "return an error if collection has items" in new scope() {
        serviceSpy.itemCount(collection.id) returns 1
        serviceSpy.delete(collection.id).isFailure === true
      }

      "not remove the collection if it has items" in new scope() {
        serviceSpy.itemCount(collection.id) returns 1
        serviceSpy.delete(collection.id)
        serviceSpy.findOneById(collection.id) !== None
      }

      //TODO difficult to throw an error
      "return an error when service calls return SalatRemoveError" in pending

      //TODO difficult to throw an error
      "return an error when service calls return SalatDAOUpdateError" in pending

      //TODO roll back not implemented in service
      "roll back when organization could not be updated" in pending

      //TODO roll back not implemented in service
      "roll back when items could not be updated" in pending
    }

    "listCollectionsByOrg" should {

      class scope extends withCollection

      "list 1 collection for the new org" in new scope {
        service.listCollectionsByOrg(org.id).length === 1
        service.listCollectionsByOrg(org.id).toSeq === Seq(collection)
      }
    }

    "create" should {

      class scope extends baseScope {

        override def after: Any = {
          services.orgService.delete(org.id)
        }
      }

      "create a new collection" in new scope {
        services.contentCollectionService.create("my-new-collection", org)
        services.contentCollectionService.getCollections(org.id, Permission.Write).isSuccess === true
      }
    }
  }
}
