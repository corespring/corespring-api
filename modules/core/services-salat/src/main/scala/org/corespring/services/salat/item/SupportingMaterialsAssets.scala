package org.corespring.platform.core.services.item

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ DeleteObjectsRequest, ObjectMetadata, S3Object }
import grizzled.slf4j.Logger
import org.corespring.assets.AssetKeys
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.resource.{ Resource, StoredFile }

import scalaz.{ Failure, Success, Validation }

class SupportingMaterialsAssets[A](s3: AmazonS3, bucketHolder: Bucket, assetKeys: AssetKeys[A]) {

  val bucket = bucketHolder.bucket

  private lazy val logger = Logger(classOf[SupportingMaterialsAssets[A]])

  def deleteDir(id: A, resource: Resource): Validation[String, Resource] = Validation.fromTryCatch {

    logger.debug(s"[deleteDir] id=$id, resource=${resource.name}")
    val parent = assetKeys.supportingMaterialFolder(id, resource.name)
    val listing = s3.listObjects(bucket, parent)
    import scala.collection.JavaConversions._
    val keys: List[String] = listing.getObjectSummaries.toList.map { s =>
      s.getKey
    }
    val allKeys: List[String] = keys :+ parent
    val request = new DeleteObjectsRequest(bucket).withKeys(allKeys: _*)
    s3.deleteObjects(request)
    resource
  }.leftMap { e =>
    e.printStackTrace()
    e.getMessage
  }

  def getS3Object(id: A, materialName: String, filename: String, etag: Option[String]): Option[S3Object] = {
    logger.debug(s"[getS3Object] id=$id, resource=$materialName, file=$filename")
    val key = assetKeys.supportingMaterialFile(id, materialName, filename)
    Some(s3.getObject(bucket, key))
  }

  def upload(id: A, resource: Resource, file: StoredFile, bytes: Array[Byte]): Validation[String, StoredFile] = {
    logger.debug(s"[upload] id=$id, resource=${resource.name}, file=${file.name}")
    val key = assetKeys.supportingMaterialFile(id, resource.name, file.name)
    val metadata = new ObjectMetadata()
    metadata.setContentType(file.contentType)
    metadata.setContentLength(bytes.length.toLong)
    val stream = new ByteArrayInputStream(bytes)
    try {
      s3.putObject(bucket, key, stream, metadata)
      Success(file)
    } catch {
      case t: Throwable => {
        logger.warn(s"function=upload, bucket=$bucket, key=$key - Error uploading: ${t.getMessage}")

        if (logger.isDebugEnabled) {
          t.printStackTrace()
        }
        Failure(s"An error occurred: ${t.getMessage}")
      }
    } finally {
      stream.close()
    }
  }

  def deleteFile(id: A, resource: Resource, filename: String): Validation[String, String] = Validation.fromTryCatch {
    logger.debug(s"[deleteFile] id=$id, resource=${resource.name}, file=${filename}")
    val key = assetKeys.supportingMaterialFile(id, resource.name, filename)
    s3.deleteObject(bucket, key)
    filename
  }.leftMap { _.getMessage }
}
