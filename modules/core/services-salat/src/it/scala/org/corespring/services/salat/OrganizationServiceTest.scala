package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.{ContentCollection, MetadataSetRef, ContentCollRef, Organization}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.{PlatformServiceError, GeneralError}
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

import scalaz.{Validation, Failure, Success}

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait TestWrapper extends BeforeAfter {

    lazy val service = services.orgService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId)
    lazy val org = Organization(
      name = "orgservice-test-org",
      id = orgId,
      contentcolls = Seq(ContentCollRef(collectionId = collectionId)))
    lazy val setId: ObjectId = ObjectId.get

    def after: Any = service.delete(orgId)

    def before: Any = service.insert(org, None)

    def assertCCR(ccr:ContentCollRef, collId:ObjectId, p: Permission, org: ObjectId) = {
      ccr.collectionId === collId
      ccr.pval === p.value
      service.hasCollRef(orgId, ccr) === true
    }
  }

  "addCollection" should {

    "add new collection id with read permission" in new TestWrapper {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Read) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(ccr) => assertCCR(ccr, newCollectionId, Permission.Read, orgId)
      }
    }

    "add new collection id with write permission" in new TestWrapper {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Write) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(ccr) => assertCCR(ccr, newCollectionId, Permission.Write, orgId)
      }
    }

    "fail to add collection id, if it does exists in content coll refs" in new TestWrapper {
      service.addCollection(orgId, collectionId, Permission.Read) match {
        case Failure(e) => success
        case Success(ccr) => failure(s"Unexpected success")
      }
    }
  }

  "addCollectionReference" should {
    "add content coll reference to org" in new TestWrapper {
      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertCCR(ref, newCollectionId, Permission.Write, orgId)
      }
    }
    "not fail when content coll ref is duplicate" in new TestWrapper {
      val ref = ContentCollRef(collectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertCCR(ref, collectionId, Permission.Write, orgId)
      }
    }
  }

  "addMetadataSet" should {
    "if the set does not exist" should {
      "return an error when checkExistence is the default" in new TestWrapper {
        service.addMetadataSet(orgId, setId) must equalTo(Failure("couldn't find the metadata set"))
      }
      "return an error when checkExistence = true" in new TestWrapper {
        service.addMetadataSet(orgId, setId, true) must equalTo(Failure("couldn't find the metadata set"))
      }
      "if checkExistence is false" should {
        "return the new ref" in new TestWrapper {
          service.addMetadataSet(orgId, setId, false) must equalTo(Success(MetadataSetRef(setId, true)))
        }
        "add a metadataset to the org" in new TestWrapper {
          service.addMetadataSet(orgId, setId, false)
          service.findOneById(org.id).map {
            org => org.metadataSets.length === 1
          }.getOrElse(failure("didn't find org"))
        }
      }
    }
    "if the set exists" should {
      trait WithSetScope extends TestWrapper {
        val metadataSet = new MetadataSet("","","")
        //TODO Can we insert a metadata set only? Update is not the correct method
        services.metadataSetService.update(metadataSet)

        override def after = {
          //TODO Can we remove a metadata set only? Delete also tries to update the org
          services.metadataSetService.delete(ObjectId.get, metadataSet.id)
          super.after
        }
      }
      "return no error when checkExistence is default" in new WithSetScope {
        service.addMetadataSet(orgId, metadataSet.id).isSuccess === true
      }
      "return no error when checkExistence is true" in new WithSetScope {
        service.addMetadataSet(orgId, metadataSet.id, true).isSuccess === true
      }
    }
  }

  "addPublicCollectionToAllOrgs" in pending
  "canAccessCollection" in pending
  "canAccessCollection" in pending
  "changeName" in pending
  "defaultCollection" in pending
  "defaultCollection" in pending
  "delete" in pending
  "deleteCollectionFromAllOrganizations" in pending
  "disableCollection" in pending
  "enableCollection" in pending
  "findOneById" in pending
  "findOneByName" in pending
  "getDefaultCollection" in pending
  "getOrgPermissionForItem" in pending
  "getOrgsWithAccessTo" in pending
  "getPermissions" in pending
  "getTree" in pending
  "hasCollRef" in pending
  "insert" in pending
  "isChild" in pending
  "orgsWithPath" in pending
  "removeCollection" in pending
  "removeMetadataSet" in pending
  "updateCollection" in pending
  "updateOrganization" in pending

  "changeName" should {

    "change the org name" in new TestWrapper {
      service.changeName(org.id, "update")
      service.findOneById(org.id).map(_.name) === Some("update")
    }

    "return an error if no org is found" in new TestWrapper {
      service.changeName(ObjectId.get, "update").swap.toOption.get must haveClass[GeneralError]
    }
  }


  "removeMetadataSet" should {

    "remove a metadataset" in new TestWrapper {
      service.addMetadataSet(orgId, setId, false)
      service.findOneById(org.id).map { org => org.metadataSets.length === 1 }.getOrElse(failure("didn't find org"))
      service.removeMetadataSet(orgId, setId)
      service.findOneById(org.id).map { org => org.metadataSets.length === 0 }.getOrElse(failure("didn't find org"))
    }

  }

  "setCollectionEnabledStatus" should {
    "set the status to true" in new TestWrapper {
      service.enableCollection(org.id, collectionId)
      service.findOneById(org.id).get
        .contentcolls
        .find(_.collectionId == collectionId)
        .map(_.enabled) === Some(true)
    }

    "set the status to false" in new TestWrapper {
      service.enableCollection(org.id, collectionId)
      service.disableCollection(org.id, collectionId)
      service.findOneById(org.id).get
        .contentcolls
        .find(_.collectionId == collectionId)
        .map(_.enabled) === Some(false)
    }
  }

  "updateOrganization" should {
    "update the org in the db" in new TestWrapper {
      val update = org.copy(name = "update")
      service.updateOrganization(update)
      service.findOneById(update.id).map(_.name) === Some("update")
    }
  }

}

