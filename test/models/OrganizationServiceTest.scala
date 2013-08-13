package models

import models.metadata.MetadataSetServiceImpl
import org.bson.types.ObjectId
import org.specs2.mock.Mockito
import org.specs2.mutable.{BeforeAfter, Specification}
import tests.PlaySingleton

class OrganizationServiceTest extends Specification with Mockito{

  PlaySingleton.start()

  class TestWrapper extends BeforeAfter{

    def orgServiceWithMockMetadataService(findResult:Option[MetadataSet]) : OrganizationService = {
      val service : OrganizationService = new OrganizationImpl {
        def metadataSetService: MetadataSetServiceImpl = {
          val m = mock[MetadataSetServiceImpl]
          m.findOneById(setId) returns findResult
          m
        }
      }
      service
    }

    def serviceImpl : OrganizationImpl = orgServiceWithMockMetadataService(None).asInstanceOf[OrganizationImpl]

    lazy val orgId : ObjectId = ObjectId.get
    lazy val org = Organization("orgservice-test-org", Seq(), Seq(), Seq(), orgId )
    lazy val setId : ObjectId = ObjectId.get

    def after: Any = serviceImpl.removeById(orgId)

    def before: Any = serviceImpl.save(org)

  }

  "org service" should{
    "return an error string if the set doesn't exist" in new TestWrapper {
      orgServiceWithMockMetadataService(None).addMetadataSet(orgId, setId, true) match {
        case Left(e) => e === "couldn't find the metadata set"
        case Right(ref) => failure("didn't work")
      }
    }

    "not return an error string if the set doesn't exist but check existence is false" in new TestWrapper{
      orgServiceWithMockMetadataService(None).addMetadataSet(orgId, setId, false) match {
        case Left(e) => failure("no error should be returned")
        case Right(ref) => success
      }
    }

    "add a metadataset" in new TestWrapper {
      serviceImpl.addMetadataSet(orgId, setId, false)
      serviceImpl.findOneById(org.id).map{ org => org.metadataSets.length === 1 }.getOrElse(failure("didn't find org"))
    }

    "remove a metadataset" in new TestWrapper {
      serviceImpl.addMetadataSet(orgId, setId, false)
      serviceImpl.removeMetadataSet(orgId, setId)
      serviceImpl.findOneById(org.id).map{ org => org.metadataSets.length === 0 }.getOrElse(failure("didn't find org"))
    }
  }

}
