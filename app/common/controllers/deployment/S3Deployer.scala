package common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{SetBucketPolicyRequest, ObjectMetadata}
import com.ee.assets.deployment.{ContentInfo, Deployer}
import common.log.PackageLogging
import common.utils.string
import java.io.{InputStream, ByteArrayInputStream}
import java.util.Date
import scala.collection.mutable

/** An implementation of the Assets-Loader Deployer trait that writes the assets to s3 and returns the s3 url back */
class S3Deployer(client: Option[AmazonS3], bucket: String) extends Deployer with PackageLogging {

  def createBucketIfRequired = client.map {
    s3 =>
      try {
        s3.getBucketPolicy(bucket)
      } catch {
        case e: Throwable => {
          Logger.debug("creating bucket: " + bucket)
          s3.createBucket(bucket)
          val text = string.interpolate(S3Deployer.policyTemplate, string.replaceKey(Map("bucket" -> bucket)), string.DollarRegex)
          val request = new SetBucketPolicyRequest(bucket, text)
          s3.setBucketPolicy(request)
        }
      }
  }

  createBucketIfRequired


  private val deployed: mutable.Map[String, String] = mutable.Map()

  def listAssets: Map[String, String] = deployed.toMap

  private def toByteArray(is:InputStream) = Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray

  def deploy(relativePath: String, lastModified: Long, stream : => InputStream, info: ContentInfo): Either[String, String] = {

    val deploymentPath = if(relativePath.startsWith("/")) relativePath.substring(1, relativePath.length) else relativePath

    Logger.debug("deploy: " + deploymentPath + ", lastModified: " + lastModified)
    def key: String = deploymentPath + ":" + lastModified

    def checkS3: Option[String] = client.map {
      s3 =>
        try {
          Logger.debug("check s3 for: " + deploymentPath )
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
            Logger.debug("upload file: " + deploymentPath )
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
