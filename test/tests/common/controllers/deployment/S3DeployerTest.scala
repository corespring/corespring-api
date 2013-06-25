package tests.common.controllers.deployment

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.SetBucketPolicyRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client}
import com.ee.assets.deployment.ContentInfo
import com.typesafe.config.ConfigFactory
import common.controllers.deployment.S3Deployer
import org.specs2.mutable.{Before, Specification}
import play.api.Play
import tests.PlaySingleton
import common.utils.string
import java.io._
import java.util.zip.GZIPInputStream
import scala.Left
import com.ee.assets.deployment.ContentInfo
import scala.Right
import scala.Some

class S3DeployerTest extends Specification {


  PlaySingleton.start()

  sequential

  lazy val client: AmazonS3Client = new AmazonS3Client(new AWSCredentials {
    def getAWSAccessKeyId: String = ConfigFactory.load().getString("AMAZON_ACCESS_KEY")

    def getAWSSecretKey: String = ConfigFactory.load().getString("AMAZON_ACCESS_SECRET")
  })

  val bucket = ConfigFactory.load().getString("AMAZON_TEST_BUCKET")

  "s3 deployer" should {

    "deploy files" in new RemoveFileBefore(client, bucket, "test/tests/files/one.js") {

      import play.api.Play.current

      val file = Play.getFile(path)
      val source: String = scala.io.Source.fromFile(file.getAbsolutePath).getLines().mkString("\n")
      val inputStream = new ByteArrayInputStream(source.getBytes("UTF-8"))
      deployer.deploy(path, file.lastModified(), inputStream, ContentInfo(contentType = "text/javascript")) match {
        case Left(e) => failure(e)
        case Right(p) => {
          p === S3Deployer.getUrl(bucket, prefixPath(path))
        }
      }

      deployer.deploy(path, file.lastModified(), inputStream, ContentInfo(contentType = "text/javascript"))

      deployer.listAssets === Map(prefix + "/" + path + ":" + file.lastModified() -> S3Deployer.getUrl(bucket, prefixPath(path)))
    }

    "deploy gz file" in new RemoveFileBefore(client, bucket, "test/tests/files/cs-common.min.gz.js"){
       import play.api.Play.current

      val file = Play.getFile(path)

      def gzip(file: File): InputStream =  new BufferedInputStream(new FileInputStream(path))

      deployer.deploy(path, file.lastModified(), gzip(file), ContentInfo(contentType = "text/javascript", contentEncoding = Some("gzip"))) match {
        case Left(e) => failure(e)
        case Right(p) => {
          println("url: " + p)
          p === S3Deployer.getUrl(bucket, prefixPath(path))
        }
      }
    }
  }
}

class RemoveFileBefore(val client: AmazonS3, val bucket: String, val path: String) extends Before {

  lazy val prefix = "my_test_prefix"
  lazy val deployer = new S3Deployer(Some(client), bucket, prefix )


  def prefixPath(p:String) = prefix + "/" + p

  def before {
    try {
      client.getObject(bucket, path)
      client.deleteObject(bucket, path)
    } catch {
      case e: Throwable => {
        client.createBucket(bucket)
        val text = string.interpolate(S3Deployer.policyTemplate, string.replaceKey(Map("bucket" -> bucket)), string.DollarRegex)
        val request = new SetBucketPolicyRequest(bucket, text)
        client.setBucketPolicy(request)
      }
    }
  }
}
