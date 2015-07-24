package org.corespring.services.salat.metadata

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.metadata.{ MetadataSet, SchemaMetadata }
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

class MetadataSetServiceTest extends ServicesSalatIntegrationTest with Mockito {

  lazy val service = services.metadataSet

  /**
   * Note: Always use traits for BeforeAfter
   */
  trait SetWrapper extends BeforeAfter {
    lazy val orgId = {
      val org = Organization(name = "test org", isRoot = true)
      val result = services.org.insert(org, None)
      org.id
    }

    def sToSchema(s: Seq[String]): Seq[SchemaMetadata] = s.map(SchemaMetadata(_))

    val newSet = MetadataSet("some_org_metadata",
      "http://some-org/metadata-editor",
      "Some Org Metadata",
      false,
      sToSchema(Seq("color", "shape", "curve")),
      ObjectId.get)

    override def after: Any = {
      service.delete(orgId, newSet.id)
      services.org.delete(orgId)
    }

    def before: Any = {}
  }

  "metadata set service" should {

    "create" in new SetWrapper {
      service.create(orgId, newSet) match {
        case Left(e) => failure(e)
        case Right(set) => success
      }
    }

    "list" in new SetWrapper {
      service.list(orgId).length === 0
      println(s"org id : $orgId")
      val result = service.create(orgId, newSet)
      println(s"result: $result")
      service.list(orgId).length === 1
    }

    "update" in new SetWrapper {
      service.create(orgId, newSet)
      val copy = newSet.copy(metadataKey = "new_key")
      service.update(copy)
      service.findByKey("new_key") === Some(copy)
    }

    "delete" in new SetWrapper {
      service.create(orgId, newSet)
      service.findByKey(newSet.metadataKey) === Some(newSet)
      service.delete(orgId, newSet.id)
      service.findByKey(newSet.metadataKey) === None
    }
  }

}
