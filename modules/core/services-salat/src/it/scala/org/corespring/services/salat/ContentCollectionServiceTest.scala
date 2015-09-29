package org.corespring.services.salat

import org.corespring.models.auth.Permission
import org.corespring.models.{ContentCollRef, ContentCollection, Organization}
import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.specification.{After, Scope}

import scalaz.Success

class ContentCollectionServiceTest
  extends ServicesSalatIntegrationTest {

  def calling(n:String) = s"when calling $n"

  "ContentCollectionService" should {

    calling("insertCollection") should {
      "work" in pending
    }

    calling("shareItems") should {
      "work" in pending
    }

    calling("getDefaultCollection") should {
      "work" in pending
    }

    calling("unShareItems") should {
      "work" in pending
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

    calling("isPublic") should {
      "work" in pending
    }

    calling("isAuthorized") should {
      "work" in pending
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


    "listCollectionsByOrg" should {

      trait scope extends After{

        val service = services.contentCollectionService
        val org = services.orgService.insert(Organization("test-org"), None).toOption.get
        val collection = ContentCollection("test-coll", org.id)
        service.insertCollection(org.id, collection, Permission.Read)

        override def after: Any = {
          services.orgService.delete(org.id)
          service.delete(collection.id)
        }
      }

      "list 1 collection for the new org" in new scope{
        service.listCollectionsByOrg(org.id).length must_== 1
        service.listCollectionsByOrg(org.id).toSeq must_== Seq(collection)
      }
    }

    "create" should {

      trait scope extends After {
        lazy val org = services.orgService.insert(Organization("test-org"), None).toOption.get
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
