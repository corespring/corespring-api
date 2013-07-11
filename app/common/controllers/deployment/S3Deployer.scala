package common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.ee.assets.deployment.ContentInfo
import com.ee.assets.deployment.Deployer
import common.log.PackageLogging
import common.utils.string
import java.io.{InputStream, ByteArrayInputStream}
import java.util.Date
import scala.Left
import scala.Right
import scala.Some
import scala.collection.mutable

/** An implementation of the Assets-Loader Deployer trait that writes the assets to s3 and returns the s3 url back */
class S3Deployer(client: Option[AmazonS3], bucket: String, prefix: String) extends Deployer with PackageLogging {

  require(!prefix.startsWith("/"), "the prefix cannot start with a leading /")

  createCleanBucket

  private val deployed: mutable.Map[String, String] = mutable.Map()

  def listAssets: Map[String, String] = deployed.toMap


  def deploy(relativePath: String, lastModified: Long, stream: => InputStream, info: ContentInfo): Either[String, String] = {

    val deploymentPath = (prefix + "/" + relativePath).replaceAll("//", "/")

    Logger.debug("deploy: " + deploymentPath + ", lastModified: " + lastModified)
    def key: String = deploymentPath + ":" + lastModified

    def checkS3: Option[String] = client.map {
      s3 =>
        try {
          Logger.debug("check s3 for: " + deploymentPath)
          s3.getObject(bucket, deploymentPath)
          val url = S3Deployer.getUrl(bucket, deploymentPath)
          deployed += (key -> url)
          Some(url)
        } catch {
          case e: Throwable => None
        }
    }.getOrElse(None)

    def uploadFileAndReturnUrl: Either[String, String] = {
      client.map {
        s3 =>
          try {
            Logger.debug("upload file: " + deploymentPath)
            val metadata = new ObjectMetadata()
            metadata.setLastModified(new Date(lastModified))
            metadata.setContentType(info.contentType)
            info.contentEncoding.foreach {
              metadata.setContentEncoding(_)
            }
            val bytes = toByteArray(stream)
            metadata.setContentLength(bytes.length)
            val bytesInputStream = new ByteArrayInputStream(bytes)
            s3.putObject(bucket, deploymentPath, bytesInputStream, metadata)
            deployed += (key -> S3Deployer.getUrl(bucket, deploymentPath))
            Right(deployed.get(key).get)
          }
          catch {
            case e: Throwable => Left(e.getMessage)
          }
      }.getOrElse(Left("The amazon client isn't initialized"))
    }

    val url: Option[String] = deployed.get(key).orElse(checkS3)
    url.map(Right(_)).getOrElse(uploadFileAndReturnUrl)
  }

  private def toByteArray(is: InputStream) = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray

  /** Try and delete everything from an existing bucket - if that fails - create a new bucket and set the access policy.
   */
  private def createCleanBucket = client.map {
    s3 =>
      try {
        deleteAllFromBucket(s3)
      } catch {
        case e: Throwable => {
          Logger.debug("creating new bucket: " + bucket)
          s3.createBucket(bucket)
          val text = string.interpolate(S3Deployer.policyTemplate, string.replaceKey(Map("bucket" -> bucket)), string.DollarRegex)
          val request = new SetBucketPolicyRequest(bucket, text)
          s3.setBucketPolicy(request)
        }
      }
  }

  private def deleteAllFromBucket(s3:AmazonS3) {
    import scala.collection.JavaConversions._
    Logger.debug("List object in bucket: " + bucket )
    val summaries : List[S3ObjectSummary] = s3.listObjects(bucket).getObjectSummaries().toList
    val keys = summaries.map( s => new DeleteObjectsRequest.KeyVersion(s.getKey))
    val deleteRequest = new DeleteObjectsRequest(bucket)
    deleteRequest.setKeys(keys)
    val result : DeleteObjectsResult = s3.deleteObjects(deleteRequest)
    val deletedKeys = result.getDeletedObjects().toList.map(_.getKey)
    Logger.debug("deleted: " + deletedKeys.mkString(", "))
  }
}

object S3Deployer {

  def getUrl(bucket: String, path: String): String = {
    val template = "s3.amazonaws.com/${bucket}/${path}"
    "//" + string.interpolate(template, string.replaceKey(Map("bucket" -> bucket, "path" -> path)), string.DollarRegex).replaceAll("//", "/")
  }

  val policyTemplate = """{
                         |  "Version":"2008-10-17",
                         |  "Statement":[{
                         |	"Sid":"AllowPublicRead",
                         |		"Effect":"Allow",
                         |	  "Principal": {
                         |			"AWS": "*"
                         |		 },
                         |	  "Action":["s3:GetObject"],
                         |	  "Resource":["arn:aws:s3:::${bucket}/*"
                         |	  ]
                         |	}
                         |  ]
                         |}
                       """.stripMargin

}
