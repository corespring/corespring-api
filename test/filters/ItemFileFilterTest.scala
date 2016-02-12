package filters

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectResult, S3Object, S3ObjectInputStream}
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpRequestBase
import org.corespring.v2.player.cdn.CdnResolver
import org.corespring.v2.sessiondb.{SessionServices, SessionService}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.SimpleResult
import play.api.test.{FakeRequest, PlaySpecification}

import scala.concurrent.{ExecutionContext, Future}

class ItemFileFilterTest extends PlaySpecification with Mockito {

  import ExecutionContext.Implicits.global

  private trait scope extends Scope {

    val mockSessionServices = {
      val m = SessionServices(mock[SessionService], mock[SessionService])
      m.main.load("123") returns Some(Json.obj(
        "itemId" -> "123456789012345678901234:1"
      ))
      m.preview.load("123") returns None
      m
    }

    val mockCdnResolver = {
      val m = mock[CdnResolver]
      m.resolveDomain("/123456789012345678901234/1/data/file.jpeg") returns "CDN://123456789012345678901234/1/data/file.jpeg"
      m
    }

    val mockLogger = {
      val m = mock[Logger]
      m
    }

    def fr = FakeRequest("GET", "/v2/player/player/session/123/file.jpeg")

    lazy val filter = new ItemFileFilter {

      override implicit def ec: ExecutionContext = ExecutionContext.global

      override def cdnResolver: CdnResolver = mockCdnResolver

      override def sessionServices: SessionServices = mockSessionServices

      override lazy val logger = mockLogger
    }

    def underlyingResult = {
      Ok("alert('hi');").withHeaders(
        CONTENT_LENGTH -> "underlying".getBytes("utf-8").length.toString,
        CONTENT_TYPE -> "text/js")
    }

    lazy val result = filter.apply(rh => Future { underlyingResult })(fr)
  }

  "apply" should {

    "not redirect" should {
      "if request method is not GET" in new scope {
        override val fr = FakeRequest("POST", "/v2/player/player/session/123/file.jpeg")
        status(result) must_== OK
      }
      "if request url does not match" in new scope {
        override val fr = FakeRequest("GET", "/something/file.jpeg")
        status(result) must_== OK
      }
      "if file is index.html" in new scope {
        override val fr = FakeRequest("GET", "/v2/player/player/session/123/index.html")
        status(result) must_== OK
      }
    }

    "redirect" should {
      "return status temporary-redirect" in new scope {
        status(result) must_== TEMPORARY_REDIRECT
      }
      "add the cdn domain" in new scope {
        redirectLocation(result) must_== Some("CDN://123456789012345678901234/1/data/file.jpeg")
      }
    }

    "fail" should {
      "if session service does not return session json" in new scope {
        mockSessionServices.main.load("123") returns None
        status(result) must_== INTERNAL_SERVER_ERROR
      }
      "if session does not contain itemId" in new scope {
        mockSessionServices.main.load("123") returns Some(Json.obj(
          "XXX" -> "123456789012345678901234:1"
        ))
        status(result) must_== INTERNAL_SERVER_ERROR
      }
      "if itemId is not an ObjectId" in new scope {
        mockSessionServices.main.load("123") returns Some(Json.obj(
          "itemId" -> "not an objectId"
        ))
        status(result) must_== INTERNAL_SERVER_ERROR
      }
      "if itemId does not have a version" in new scope {
        mockSessionServices.main.load("123") returns Some(Json.obj(
          "itemId" -> "123456789012345678901234"
        ))
        status(result) must_== INTERNAL_SERVER_ERROR
      }
    }


  }
}

