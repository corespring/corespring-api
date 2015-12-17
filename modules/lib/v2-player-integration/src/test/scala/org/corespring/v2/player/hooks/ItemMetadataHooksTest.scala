package org.corespring.v2.player.hooks

import java.io.ByteArrayInputStream

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{Binary, CreateBinaryMaterial}
import org.corespring.models.item.resource.{Resource, StoredFileDataStream}
import org.corespring.models.metadata.{Metadata, MetadataSet}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.SupportingMaterialsService
import org.corespring.services.metadata.{MetadataSetService, MetadataService}
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{AuthMode, MockFactory, OrgAndOpts}
import org.corespring.v2.errors.Errors.generalError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.integration.hooks.beFutureErrorCodeMessage
import org.corespring.v2.player.{V2PlayerExecutionContext, V2PlayerIntegrationSpec}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.specs2.time.NoTimeConversions
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scalaz.{Failure, Success, Validation}

class ItemMetadataHooksTest extends V2PlayerIntegrationSpec with Mockito with MockFactory {

  val orgAndOpts = Success(mockOrgAndOpts(AuthMode.AccessToken))
  val vid = VersionedId(ObjectId.get, Some(0))

  trait scope
    extends Scope {

    def orgAndOptsResult: Validation[V2Error, OrgAndOpts] = orgAndOpts

    val mockAuth = {
      val m = mock[ItemAuth[OrgAndOpts]]
      m.loadForWrite(any[String])(any[OrgAndOpts]) returns Success(mockItem)
      m.canWrite(any[String])(any[OrgAndOpts]) returns Success(true)
      m
    }

    val mockMetadataService =
    {
      val m = mock[MetadataService]
      m.get(any[VersionedId[ObjectId]], any[Seq[String]]) returns Seq(Metadata("key", Map()))
      m
    }

    val mockMetadataSetService = {
      val m = mock[MetadataSetService]
      m.list(any[ObjectId]) returns Seq(MetadataSet("key","editorUrl","editorLabel"))
      m
    }

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

    val hooks = new ItemMetadataHooks(mockMetadataService, mockMetadataSetService, getOrgAndOptions, containerExecutionContext)

  }

  implicit val req = FakeRequest("", "")

  "get" should {
    "fetch the metadata" in new scope {
      hooks.get(vid.toString).map(_.isRight) must equalTo(true).await
    }

  }

}
