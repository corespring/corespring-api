package models.metadata

import models._
import org.bson.types.ObjectId
import org.specs2.mock.Mockito
import org.specs2.mutable.{BeforeAfter, Specification}
import tests.PlaySingleton
import models.MetadataSetRef
import models.SchemaMetadata
import scala.Some

class MetadataSetServiceImplTest extends Specification with Mockito {

  PlaySingleton.start()


  class SetWrapper extends BeforeAfter {
    val orgId = ObjectId.get()

    val service = new MetadataSetServiceImpl {
      def orgService: OrganizationService = {
        val m = mock[OrganizationService]
        import org.mockito.Matchers._
        m.findOneById(anyObject()) returns Some(Organization(metadataSets = Seq(MetadataSetRef(newSet.id, true))))
        m.addMetadataSet(anyObject(), anyObject(), anyBoolean()) returns Right(MetadataSetRef(newSet.id, true))
        m
      }
    }

    def sToSchema(s: Seq[String]): Seq[SchemaMetadata] = s.map(SchemaMetadata(_))

    val newSet = MetadataSet("some_org_metadata",
      "http://some-org/metadata-editor",
      "Some Org Metadata",
      false,
      sToSchema(Seq("color", "shape", "curve")),
      ObjectId.get)

    def after: Any = {
      service.delete(orgId, newSet.id)
    }

    def before: Any = { }
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
      service.create(orgId, newSet)
      service.list(orgId).length === 1
    }

    "update" in new SetWrapper{
      service.create(orgId, newSet)
      val copy = newSet.copy(metadataKey = "new_key")
      service.update( copy )
      service.findByKey("new_key") ===  Some(copy)
    }

    "delete" in new SetWrapper{
      service.create(orgId, newSet)
      service.delete(orgId, newSet.id)
      service.findByKey(newSet.metadataKey) === None
    }
  }

}
