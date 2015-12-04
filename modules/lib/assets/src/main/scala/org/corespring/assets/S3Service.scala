package org.corespring.assets

import com.amazonaws.services.s3.AmazonS3
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }

trait S3ServiceClient {
  def s3Service: CorespringS3Service
}

trait CorespringS3Service extends S3Service {
  def copyFile(bucket: String, keyName: String, newKeyName: String)

  def online: Boolean

  def getClient: AmazonS3
}

class CorespringS3ServiceExtended(client: AmazonS3)
  extends ConcreteS3Service(client)
  with CorespringS3Service {

  def getClient = client

  def copyFile(bucket: String, keyName: String, newKeyName: String) = client.copyObject(bucket, keyName, bucket, newKeyName)

  def online: Boolean = try {
    client.listBuckets().size() > 0
    true
  } catch {
    case e: Throwable => false
  }
}

