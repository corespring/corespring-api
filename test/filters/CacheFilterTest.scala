package filters

import java.io.InputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ S3ObjectInputStream, ObjectMetadata, PutObjectResult, S3Object }
import org.apache.commons.io.IOUtils
import org.apache.http.HttpRequest
import org.apache.http.client.methods.HttpRequestBase
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import play.api.mvc.Results._
import play.api.mvc.SimpleResult
import play.api.test.{ FakeRequest, PlaySpecification }

import scala.concurrent.{ ExecutionContext, Future }

class CacheFilterTest extends PlaySpecification with Mockito {

  import ExecutionContext.Implicits.global

  private trait scope extends Scope {

    val mockMetadata = {
      val m = mock[ObjectMetadata]
      m.getContentType returns "text/js"
      m
    }

    val mockS3Object = {
      val m = mock[S3Object]
      m.getObjectMetadata returns mockMetadata
      val in = IOUtils.toInputStream("hi", "UTF-8")
      m.getObjectContent returns new S3ObjectInputStream(in, mock[HttpRequestBase])
      m
    }

    val mockPutResult = {
      val m = mock[PutObjectResult]
      m.getETag returns "etag"
      m
    }

    val mockS3 = {
      val m = mock[AmazonS3]
      m.getObject(any[String], any[String]) returns mockS3Object
      m.getObjectMetadata(any[String], any[String]) returns mockMetadata
      m.putObject(any[String], any[String], any[InputStream], any[ObjectMetadata]) returns mockPutResult
      m
    }

    def intercept: Boolean = true

    def fr = FakeRequest("GET", "path")

    lazy val filter = new CacheFilter {

      override def bucket: String = "bucket"

      override def appVersion: String = "version"

      override implicit def ec: ExecutionContext = ExecutionContext.global

      override def s3: AmazonS3 = mockS3

      override def intercept(path: String): Boolean = scope.this.intercept

      override def gzipEnabled: Boolean = false

      override def futureQueue: FutureQueuer = new FutureQueuer {
        override def queued(key: String)(body: => Future[SimpleResult]): Future[SimpleResult] = body
      }
    }

    def underlyingResult = {
      Ok("alert('hi');").withHeaders(
        CONTENT_LENGTH -> "underlying".getBytes("utf-8").length.toString,
        CONTENT_TYPE -> "text/js")
    }

    lazy val result = filter.apply(rh => Future { underlyingResult })(fr)
  }

  "apply" should {

    "not run if intercept returns false" in new scope {
      override val intercept = false
      status(result) must_== OK
      there was no(mockS3).getObjectMetadata(any[String], any[String])
      there was no(mockS3).getObject(any[String], any[String])
    }

    "when intercepted" should {
      trait intercepted extends scope {
      }

      "intercept and call s3.getObject" in new intercepted {
        status(result) must_== OK
        there was one(mockS3).getObject("bucket", "cache-filter/version/path")
      }

      "set the encoding from the s3 object" in new intercepted {
        mockMetadata.getContentEncoding returns "gzip"
        header(CONTENT_ENCODING, result) must_== Some("gzip")
      }

      "not set the encoding if not present in the s3 object" in new intercepted {
        header(CONTENT_ENCODING, result) must_== None
      }

      "set the etag from the s3 object" in new intercepted {
        mockMetadata.getETag returns "etag"
        header(ETAG, result) must_== Some("etag")
      }

      "set the content length from the s3 object" in new intercepted {
        mockMetadata.getContentLength returns 392
        header(CONTENT_LENGTH, result) must_== Some("392")
      }

      "set the content type from the s3 object" in new intercepted {
        header(CONTENT_TYPE, result) must_== Some("text/js")
      }

      "when If-None-Match is present" should {

        "call s3.getObjectMetadata" in new intercepted {
          mockMetadata.getETag returns "my-other-etag"
          override val fr = FakeRequest("GET", "path").withHeaders(IF_NONE_MATCH -> "my-etag")
          status(result) must_== OK
          there was one(mockS3).getObjectMetadata("bucket", "cache-filter/version/path")
        }

        "return NotModified it the etag matches" in new intercepted {
          override val fr = FakeRequest().withHeaders(IF_NONE_MATCH -> "my-etag")
          mockMetadata.getETag returns "my-etag"
          status(result) must_== NOT_MODIFIED
        }

        "return Ok if the etag doesn't match" in new intercepted {
          override val fr = FakeRequest().withHeaders(IF_NONE_MATCH -> "my-etag")
          mockMetadata.getETag returns "my-other-etag"
          status(result) must_== OK
        }

        "return the etag from the put result" in new intercepted {
          override val fr = FakeRequest().withHeaders(IF_NONE_MATCH -> "my-etag")
          mockPutResult.getETag returns "put-result-etag"
          status(result) must_== OK
          header(ETAG, result) must_== Some("put-result-etag")
        }
      }
    }
  }
}

