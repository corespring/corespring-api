package tests.helpers

import controllers.{InternalError, S3Service, ConcreteS3Service}
import play.api.mvc.{BodyParser, Headers, Result}
import com.typesafe.config.ConfigFactory
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.AWSCredentials
import java.io.{File, IOException}
import com.amazonaws.{AmazonServiceException, AmazonClientException}
import scala.collection.JavaConversions._
import models.item.resource.StoredFile

object TestS3Service extends S3Service{
  def s3upload(bucket: String, keyName: String): BodyParser[Int] = ConcreteS3Service.s3upload(bucket,keyName)

  def download(bucket: String, fullKey: String, headers: Option[Headers]): Result = ConcreteS3Service.download(bucket,fullKey,headers)

  def delete(bucket: String, keyName: String): S3DeleteResponse = {
    val response = ConcreteS3Service.delete(bucket,keyName)
    S3DeleteResponse(response.success,response.key,response.msg)
  }

  def cloneFile(bucket: String, keyName: String, newKeyName: String) = ConcreteS3Service.cloneFile(bucket,keyName,newKeyName)

  def online: Boolean = ConcreteS3Service.online

  def bucket: String = ConfigFactory.load().getString("AMAZON_TEST_BUCKET")

  def files(bucket: String):Seq[String] = {
    ConcreteS3Service.getAmazonClient match {
      case Some(s3) => {
        try{
          s3.listObjects(bucket).getObjectSummaries.map(sos => sos.getKey)
        } catch {
          case e:AmazonClientException => throw e;
          case e:AmazonServiceException => throw e;
        }
      }
      case None => throw new RuntimeException("Amazon S3 not initialized")
    }
  }

  def init = {
    if (!online){
      ConcreteS3Service.init
    }
  }
  val default_image_file = "default_test_file.jpg"
  def storeDefaultImageFile = {
    ConcreteS3Service.getAmazonClient match {
      case Some(s3) => {
        try{
          s3.putObject(bucket,default_image_file,new File("files/cute-rabbit.jpg"))
        } catch {
          case e:AmazonClientException => throw e;
          case e:AmazonServiceException => throw e;
        }
      }
      case None => throw new RuntimeException("Amazon S3 not initialized")
    }
  }
}
