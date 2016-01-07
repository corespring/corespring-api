package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.errors.CollectionAuthorizationError
import org.corespring.models.auth.Permission
import org.corespring.models.{ContentCollection, ContentCollRef, Organization}
import org.corespring.models.item.Passage
import org.corespring.models.item.resource.{VirtualFile, BaseFile}
import org.corespring.passage.search._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.v2.auth.PassageAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{couldNotCreatePassage, cantFindPassageWithId, generalError}
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.RequestHeader
import play.api.test._
import play.api.test.Helpers._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scalaz.{Success, Validation, Failure}

class PassageApiTest extends Specification with Mockito {

  trait PassageApiScope extends Scope {
    val passageAuth: PassageAuth = mock[PassageAuth]
    val passageIndexService: PassageIndexService = mock[PassageIndexService]
    val orgCollectionService: OrgCollectionService = mock[OrgCollectionService]
    val v2ApiExecutionContext = V2ApiExecutionContext(ExecutionContext.global)
    def authResponse: Validation[V2Error, OrgAndOpts] = Failure(generalError("Nope", UNAUTHORIZED))
    lazy val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts] = {
      request => authResponse
    }
    val passageApi = new PassageApi(passageAuth, passageIndexService, orgCollectionService,
      v2ApiExecutionContext, getOrgAndOptionsFn)
    val passageId = new VersionedId[ObjectId](new ObjectId(), Some(0))
    def file(): BaseFile = mock[VirtualFile]
    lazy val passage = Passage(id = passageId, collectionId = new ObjectId().toString, file = file())
  }

  trait AuthorizedApiPassageScope extends PassageApiScope {
    val identity = mock[OrgAndOpts]
    override def authResponse = Success(identity)
  }

  "get" should {

    "return 401" in new PassageApiScope {
      val result = passageApi.get(passageId, None)(FakeRequest())
      status(result) must be equalTo(UNAUTHORIZED)
    }

    "authorized request" should {

      "passage does not exist" should {

        trait PassageMissingScope extends AuthorizedApiPassageScope {
          passageAuth.loadForRead(passageId.toString, None)(identity) returns Failure(cantFindPassageWithId(passageId))
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

          passageAuth.loadForRead(passageId.toString, None)(identity) returns Success(passage)
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

  "search" should {

    "return 401" in new PassageApiScope {
      val result = passageApi.search()(FakeRequest().withJsonBody(Json.obj()))
      status(result) must be equalTo(UNAUTHORIZED)
    }

    "authorized request" should {

      trait AuthorizedSearchScope extends AuthorizedApiPassageScope {
        val collections = Seq()
        identity.org returns Organization("", contentcolls = collections)
      }

      "with no results" should {

        trait SearchNoResultsScope extends AuthorizedSearchScope {
          passageIndexService.search(any[PassageIndexQuery]) returns
            Future.successful(Success(PassageIndexSearchResult(0, Seq())))
          val result = passageApi.search()(FakeRequest().withJsonBody(Json.obj()))
        }

        "return 200" in new SearchNoResultsScope {
          status(result) must be equalTo(OK)
        }

        "return total 0" in new SearchNoResultsScope {
          (contentAsJson(result) \ "total").as[Int] must be equalTo(0)
        }

        "return no hits" in new SearchNoResultsScope {
          (contentAsJson(result) \ "hits").as[Seq[JsObject]] must beEmpty
        }

      }

      "with results" should {

        trait SearchResultsScope extends AuthorizedSearchScope {
          implicit val PassageIndexHitFormat = PassageIndexHit.Format
          val searchResult = 0.to(10).toList.map{ n => {
            PassageIndexHit("", None, None, "")
          }}.toSeq
          passageIndexService.search(any[PassageIndexQuery]) returns
            Future.successful(Success(PassageIndexSearchResult(searchResult.length, searchResult)))
          val result = passageApi.search()(FakeRequest().withJsonBody(Json.obj()))
        }

        "return 200" in new SearchResultsScope {
          status(result) must be equalTo(OK)
        }

        "return total with size of results" in new SearchResultsScope {
          (contentAsJson(result) \ "total").as[Int] must be equalTo(searchResult.size)
        }

        "return hits from index service" in pending //new SearchResultsScope {
          //(contentAsJson(result) \ "hits").as[Seq[PassageIndexHit]] must be equalTo(searchResult)
        //}

      }

    }

  }

  "create" should {

    "return 401" in new PassageApiScope {
      val result = passageApi.create()(FakeRequest().withJsonBody(Json.obj()))
      status(result) must be equalTo(UNAUTHORIZED)
    }

    "authorized request" should {

      trait AuthorizedCreateScope extends AuthorizedApiPassageScope {
        val defaultCollectionId = new ObjectId()
        val writeableCollectionId = new ObjectId()
        val unwriteableCollectionId = new ObjectId()
        val defaultCollRef = ContentCollRef(collectionId = defaultCollectionId)
        val defaultCollection = ContentCollection(name = "", ownerOrgId = new ObjectId(), id = defaultCollectionId)
        val orgId = new ObjectId()
        identity.org returns Organization("", contentcolls = Seq(defaultCollRef), id = orgId)
        orgCollectionService.getDefaultCollection(orgId) returns Success(defaultCollection)
      }

      "saving succeeds" should {

        trait AuthorizedCreateScopeSaved extends AuthorizedCreateScope {
          passageAuth.insert(any[Passage])(any[OrgAndOpts], any[ExecutionContext]) answers { (args, _) =>
            val argArray = args.asInstanceOf[Array[Object]]
            val passage = argArray(0).asInstanceOf[Passage].copy(id = new VersionedId(new ObjectId(), Some(0)))
            Future.successful(Success(passage))
          }
          orgCollectionService.isAuthorized(orgId, unwriteableCollectionId, Permission.Write) returns false
          orgCollectionService.isAuthorized(orgId, writeableCollectionId, Permission.Write) returns true
        }

        trait AuthorizedCreateScopeSavedEmptyRequest extends AuthorizedCreateScopeSaved {
          val result = passageApi.create()(FakeRequest().withJsonBody(Json.obj()))
          val json = contentAsJson(result)
        }

        "return 201" in new AuthorizedCreateScopeSavedEmptyRequest {
          status(result) must be equalTo(CREATED)
        }

        "return default passage JSON" in new AuthorizedCreateScopeSavedEmptyRequest {
          (json \ "collectionId").as[String] must be equalTo(defaultCollectionId.toString)
          (json \ "file" \ "name").as[String] must be equalTo(Passage.Defaults.File.name)
          (json \ "file" \ "contentType").as[String] must be equalTo(Passage.Defaults.File.contentType)
          (json \ "file" \ "content").as[String] must be equalTo(Passage.Defaults.File.content)
        }

        "return passage JSON with identifier" in new AuthorizedCreateScopeSavedEmptyRequest {
          (json \ "id").as[String] must not beEmpty
        }

        "with collectionId provided by request" should {

          "collection/organization does not have write permission" should {

            trait AuthorizedCreateScopeSavedInvalidCollection extends AuthorizedCreateScopeSaved {
              val result = passageApi.create()(FakeRequest()
                .withJsonBody(Json.obj("collectionId" -> unwriteableCollectionId.toString)))
            }

            "return 401" in new AuthorizedCreateScopeSavedInvalidCollection {
              status(result) must be equalTo(UNAUTHORIZED)
            }

            "return message for CollectionAuthorizationError" in new AuthorizedCreateScopeSavedInvalidCollection {
              contentAsString(result) must be equalTo(CollectionAuthorizationError(orgId, Permission.Write, unwriteableCollectionId).message)
            }

          }

          "collection/organization has write permission" should {

            trait AuthorizedCreateScopeSavedValidCollection extends AuthorizedCreateScopeSaved {
              val result = passageApi.create()(FakeRequest()
                .withJsonBody(Json.obj("collectionId" -> writeableCollectionId.toString)))
              val json = contentAsJson(result)
            }

            "return 201" in new AuthorizedCreateScopeSavedValidCollection {
              status(result) must be equalTo(CREATED)
            }

            "return default passage JSON" in new AuthorizedCreateScopeSavedEmptyRequest {
              (json \ "file" \ "name").as[String] must be equalTo(Passage.Defaults.File.name)
              (json \ "file" \ "contentType").as[String] must be equalTo(Passage.Defaults.File.contentType)
              (json \ "file" \ "content").as[String] must be equalTo(Passage.Defaults.File.content)
            }

          }

        }

      }

      "saving fails" should {

        trait AuthorizedCreateScopeFailed extends AuthorizedCreateScope {
          val error = couldNotCreatePassage()
          passageAuth.insert(any[Passage])(any[OrgAndOpts], any[ExecutionContext]) returns
            Future.successful(Failure(error))
          val result = passageApi.create()(FakeRequest().withJsonBody(Json.obj()))
        }

        "return status from error" in new AuthorizedCreateScopeFailed {
          status(result) must be equalTo(error.statusCode)
        }

        "return text from error in body" in new AuthorizedCreateScopeFailed {
          contentAsString(result) must be equalTo(error.message)
        }

      }

    }

  }

  "update" should {

    "return 401" in new PassageApiScope {
      val result = passageApi.update(passageId)(FakeRequest().withJsonBody(Json.obj()))
      status(result) must be equalTo(UNAUTHORIZED)
    }


    "authorized request" should {

      trait AuthorizedUpdateScope extends AuthorizedApiPassageScope {
      }

      "be awesome" in new AuthorizedUpdateScope {
        true === true
      }
    }

  }

}
