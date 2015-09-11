package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.{ MetadataSetRef, ContentCollRef, Organization }
import org.corespring.services.errors.GeneralError
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

import scalaz.{ Failure, Success }

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait TestWrapper extends BeforeAfter {

    lazy val service = services.orgService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val org = Organization(
      name = "orgservice-test-org",
      id = orgId,
      contentcolls = Seq(ContentCollRef(collectionId = collectionId)))
    lazy val setId: ObjectId = ObjectId.get

    def after: Any = service.delete(orgId)

    def before: Any = service.insert(org, None)
  }

  "addMetadataSet" should {
    "return an error string if the set doesn't exist" in new TestWrapper {
      service.addMetadataSet(orgId, setId, true) must equalTo(Failure("couldn't find the metadata set"))
    }

    "returns the new ref - check existence is false" in new TestWrapper {
      service.addMetadataSet(orgId, setId, false) must equalTo(Success(MetadataSetRef(setId, true)))
    }

    "add a metadataset" in new TestWrapper {
      service.addMetadataSet(orgId, setId, false)
      service.findOneById(org.id).map { org => org.metadataSets.length === 1 }.getOrElse(failure("didn't find org"))
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

  "changeName" should {

    "change the org name" in new TestWrapper {
      service.changeName(org.id, "update")
      service.findOneById(org.id).map(_.name) === Some("update")
    }

    "return an error if no org is found" in new TestWrapper {
      service.changeName(ObjectId.get, "update").swap.toOption.get must haveClass[GeneralError]
    }
  }

  "updateOrganization" should {
    "update the org in the db" in new TestWrapper {
      val update = org.copy(name = "update")
      service.updateOrganization(update)
      service.findOneById(update.id).map(_.name) === Some("update")
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

}

