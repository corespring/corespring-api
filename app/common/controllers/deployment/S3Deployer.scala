package common.controllers.deployment

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{PutObjectResult, ObjectMetadata}
import com.ee.assets.deployment.{ContentInfo, Deployer}
import common.log.PackageLogging
import java.io.ByteArrayInputStream
import java.util.Date
import scala.collection.mutable
import common.utils.string


class S3Deployer(client: Option[AmazonS3], bucket: String) extends Deployer with PackageLogging {


  private val deployed: mutable.Map[String, String] = mutable.Map()

  def listAssets : Map[String,String] = deployed.toMap

  def deploy(relativePath: String, lastModified: Long, contents: => String, info: ContentInfo): Either[String, String] = {

    Logger.debug("deploy: " + relativePath + ", lastModified: " + lastModified)

    def key: String = relativePath + ":"+lastModified

    def checkS3: Option[String] = client.map {
      s3 =>
        try {
          s3.getObject(bucket, relativePath)
          val url =  S3Deployer.getUrl(bucket, relativePath)
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
            val bytes: Array[Byte] = contents.getBytes("UTF-8")
            val inputStream = new ByteArrayInputStream(bytes)
            val metadata = new ObjectMetadata()
            metadata.setLastModified(new Date(lastModified))
            metadata.setContentType(info.contentType)
            info.contentEncoding.foreach {
              metadata.setContentEncoding(_)
            }
            metadata.setContentLength(bytes.length)
            s3.putObject(bucket, relativePath, inputStream, metadata)
            deployed += (key -> S3Deployer.getUrl(bucket, relativePath))
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

  def getUrl(bucket:String, path:String) : String = {
    val template = "s3.amazonaws.com/${bucket}/${path}"
    "//" + string.interpolate( template, string.replaceKey(Map("bucket" -> bucket, "path" -> path)), string.DollarRegex).replaceAll("//", "/")
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
