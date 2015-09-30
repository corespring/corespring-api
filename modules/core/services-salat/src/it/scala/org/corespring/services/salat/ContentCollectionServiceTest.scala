package org.corespring.services.salat

import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, Organization }
import org.specs2.mutable.{ BeforeAfter, Specification }
import org.specs2.specification.{ After, Scope }

import scalaz.Success

class ContentCollectionServiceTest
  extends ServicesSalatIntegrationTest {

  def calling(n: String) = s"when calling $n"

  "ContentCollectionService" should {

    trait BaseScope extends After {
      val service = services.contentCollectionService
      val org = services.orgService.insert(Organization("test-org"), None).toOption.get
    }

    calling("insertCollection") should {
      "work" in pending
      //what does two phase commit mean?
      //what should happen if organizationService fails to add a ref to the collection?
    }

    "getDefaultCollection" should {

      class withCollections(collectionNames:String*) extends BaseScope {

        val collections = collectionNames.map( name => {
          val collection = ContentCollection(name, org.id)
          service.insertCollection(org.id, collection, Permission.Read)
          collection
        })
        val collectionIds = collections.map(c => c.id)


        override def after: Any = {
          services.orgService.delete(org.id)
          for( id <- collectionIds) {
            service.delete(id)
          }
        }
      }

      "return default collection" in new withCollections("a", "b", "c", "default") {
        service.getDefaultCollection(collectionIds) === Some(collections(3))
      }

      "return None" in new withCollections("a", "b", "c") {
        service.getDefaultCollection(collectionIds) === None
      }
    }

    calling("getContentCollRefs") should {
      "work" in pending
    }

    calling("getCollectionIds") should {
      "work" in pending
    }

    calling("addOrganizations") should {
      "work" in pending
    }

    "isPublic" should {

      class withIsPublic(isPublic:Boolean) extends BaseScope {

        val collection = ContentCollection("test-col", org.id, isPublic )
        service.insertCollection(org.id, collection, Permission.Read)

        override def after: Any = {
          services.orgService.delete(org.id)
          service.delete(collection.id)
        }
      }

      "return true when collection is public" in new withIsPublic(true){
        service.isPublic(collection.id) === true
      }

      "return false when collection is not public" in new withIsPublic(false){
        service.isPublic(collection.id) === false
      }
    }

    "isAuthorized" should {

      class withPermission(p:Permission) extends BaseScope {

        val collection = ContentCollection("test-col", org.id, false )
        service.insertCollection(org.id, collection, p)

        def canRead() = {
          service.isAuthorized(org.id, collection.id, Permission.Read)
        }

        def canWrite() = {
          service.isAuthorized(org.id, collection.id, Permission.Write)
        }

        def canNone() = {
          service.isAuthorized(org.id, collection.id, Permission.None)
        }

        override def after: Any = {
          services.orgService.delete(org.id)
          service.delete(collection.id)
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

    calling("delete") should {
      "work" in pending
    }

    calling("getPublicCollections") should {
      "work" in pending
    }

    calling("shareItemsMatchingQuery") should {
      "work" in pending
    }

    calling("itemCount") should {
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

    "listCollectionsByOrg" should {

      trait scope extends BaseScope {

        val collection = ContentCollection("test-coll", org.id)
        service.insertCollection(org.id, collection, Permission.Read)

        override def after: Any = {
          services.orgService.delete(org.id)
          service.delete(collection.id)
        }
      }

      "list 1 collection for the new org" in new scope {
        service.listCollectionsByOrg(org.id).length must_== 1
        service.listCollectionsByOrg(org.id).toSeq must_== Seq(collection)
      }
    }

    "create" should {

      trait scope extends BaseScope {

        override def after: Any = {
          services.orgService.delete(org.id)
        }
      }

      "create a new collection" in new scope {
        services.contentCollectionService.create("my-new-collection", org)

        val result = services.contentCollectionService.getCollections(org.id, Permission.Write)

        result match {
          case Success(Seq(ContentCollection("my-new-collection", org.id, false, _))) => success
          case _ => ko
        }
      }
    }
  }
}
