package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.{VirtualFile, BaseFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.PassageService
import org.corespring.v2.auth.PassageAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{cantFindPassageWithId, generalError}
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Success, Validation, Failure}

class PassageApiTest extends Specification with Mockito {

  trait PassageApiScope extends Scope {
    val passageAuth: PassageAuth = mock[PassageAuth]
    val v2ApiExecutionContext = V2ApiExecutionContext(ExecutionContext.global)
    def authResponse: Validation[V2Error, OrgAndOpts] = Failure(generalError("Nope", UNAUTHORIZED))
    lazy val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts] = {
      request => authResponse
    }
    val passageApi = new PassageApi(passageAuth, v2ApiExecutionContext, getOrgAndOptionsFn)
    val passageId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    def file(): BaseFile = mock[VirtualFile]
    lazy val passage = Passage(id = passageId, collectionId = new ObjectId().toString, file = file())
  }

  "get" should {

    "should return 401" in new PassageApiScope {
      val result = passageApi.get(passageId, None)(FakeRequest())
      status(result) must be equalTo(UNAUTHORIZED)
    }

    "authorized request" should {

      trait AuthorizedApiPassageScope extends PassageApiScope {
        val auth = mock[OrgAndOpts]
        override def authResponse = Success(auth)
      }

      "passage does not exist" should {

        trait PassageMissingScope extends AuthorizedApiPassageScope {
          passageAuth.loadForRead(passageId.toString, None)(auth) returns Failure(cantFindPassageWithId(passageId))
        }

        "return 404" in new PassageMissingScope {
          val result = passageApi.get(passageId, None)(FakeRequest())
          status(result) must be equalTo(NOT_FOUND)
        }

      }

      "passage does exist" should {

        trait HtmlPassageFoundScope extends AuthorizedApiPassageScope {
          val contentType = "text/html"
          val content = "Hey, this is some <strong>cool, cool</strong> html I wrote."
          override def file() = {
            val mockFile = mock[VirtualFile]
            mockFile.contentType returns contentType
            mockFile.content returns content
            mockFile
          }

          passageAuth.loadForRead(passageId.toString, None)(auth) returns Success(passage)
          val result = passageApi.get(passageId, None)(FakeRequest())
        }

        "return 200" in new HtmlPassageFoundScope {
          status(result) must be equalTo(OK)
        }

        "return Content-Type header from Passage file's contentType" in new HtmlPassageFoundScope {
          header("Content-Type", result) must be equalTo(Some(contentType))
        }

        "return content from Passage file's content" in new HtmlPassageFoundScope {
          contentAsString(result) must be equalTo(content)
        }

      }

    }

  }

}
