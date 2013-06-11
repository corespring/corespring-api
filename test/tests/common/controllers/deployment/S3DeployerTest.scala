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

class S3DeployerTest extends Specification {


  PlaySingleton.start()

  sequential

  lazy val client: AmazonS3Client = new AmazonS3Client(new AWSCredentials {
    def getAWSAccessKeyId: String = ConfigFactory.load().getString("AMAZON_ACCESS_KEY")

    def getAWSSecretKey: String = ConfigFactory.load().getString("AMAZON_ACCESS_SECRET")
  })

  val bucket = "s3-deployer-test-bucket"
  lazy val deployer = new S3Deployer(Some(client), bucket)

  "s3 deployer" should {

    "deploy files" in new RemoveFileBefore(client, bucket, "test/tests/files/one.js") {

      import play.api.Play.current

      val file = Play.getFile(path)
      val source: String = scala.io.Source.fromFile(file.getAbsolutePath).getLines().mkString("\n")
      deployer.deploy(path, file.lastModified(), source, ContentInfo(contentType = "text/javascript")) match {
        case Left(e) => failure(e)
        case Right(p) => {
          println("url: " + p)
          p === S3Deployer.getUrl(bucket, path)
        }
      }

      deployer.deploy(path, file.lastModified(), source, ContentInfo(contentType = "text/javascript"))

      deployer.listAssets === Map(path + ":" + file.lastModified() -> S3Deployer.getUrl(bucket, path))
    }
  }
}

class RemoveFileBefore(val client: AmazonS3, val bucket: String, val path: String) extends Before {

  def before {
    try {
      client.getObject(bucket, path)
      client.deleteObject(bucket, path)
    } catch {
      case e: Throwable => {
        client.createBucket(bucket)
        println(S3Deployer.policyTemplate)
        println(bucket)
        val text = string.interpolate(S3Deployer.policyTemplate, string.replaceKey(Map("bucket" -> bucket)), string.DollarRegex)
        val request = new SetBucketPolicyRequest(bucket, text)
        client.setBucketPolicy(request)
      }
    }
  }
}
