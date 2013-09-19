package org.corespring.web.common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.ee.assets.deployment.{ ContentInfo, Deployer }
import java.io.{ InputStream, ByteArrayInputStream }
import java.util.Date
import org.corespring.common.log.PackageLogging
import org.corespring.common.utils.string
import scala.collection.mutable

/** An implementation of the Assets-Loader Deployer trait that writes the assets to s3 and returns the s3 url back */
class S3Deployer(client: Option[AmazonS3], bucket: String, prefix: String) extends Deployer with PackageLogging {

  require(!prefix.startsWith("/"), "the prefix cannot start with a leading /")

  createCleanBucket

  private val deployed: mutable.Map[String, String] = mutable.Map()

  def listAssets: Map[String, String] = deployed.toMap

  def deploy(relativePath: String, lastModified: Long, stream: => InputStream, info: ContentInfo): Either[String, String] = {

    val deploymentPath = (prefix + "/" + relativePath).replaceAll("//", "/")

    logger.debug("deploy: " + deploymentPath + ", lastModified: " + lastModified)
    def key: String = deploymentPath + ":" + lastModified

    def checkS3: Option[String] = client.map {
      s3 =>
        try {
          logger.debug("check s3 for: " + deploymentPath)
          s3.getObject(bucket, deploymentPath)
          val url = S3Deployer.url(bucket, deploymentPath)
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
            logger.debug("upload file: " + deploymentPath)
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
            deployed += (key -> S3Deployer.url(bucket, deploymentPath))
            Right(deployed.get(key).get)
          } catch {
            case e: Throwable => Left(e.getMessage)
          }
      }.getOrElse(Left("The amazon client isn't initialized"))
    }

    val url: Option[String] = deployed.get(key).orElse(checkS3)
    url.map(Right(_)).getOrElse(uploadFileAndReturnUrl)
  }

  private def toByteArray(is: InputStream) = {
    import scala.language.postfixOps
    Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
  }

  /**
   * Try and delete everything from an existing bucket - if that fails - create a new bucket and set the access policy.
   */
  private def createCleanBucket = client.map {
    s3 =>
      try {
        logger.debug("List objects for bucket: %s".format(bucket))
        s3.listObjects(bucket)
      } catch {
        case e: Throwable => {
          logger.debug("creating new bucket: " + bucket)
          s3.createBucket(bucket)
          val text = S3Deployer.policyTemplate(bucket)
          val request = new SetBucketPolicyRequest(bucket, text)
          s3.setBucketPolicy(request)
        }
      }
  }

}

object S3Deployer {

  def url(bucket: String, path: String): String = "//" + s"s3.amazonaws.com/${bucket}/${path}".replaceAll("//", "/")

  def policyTemplate(bucket: String): String = s"""{
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
