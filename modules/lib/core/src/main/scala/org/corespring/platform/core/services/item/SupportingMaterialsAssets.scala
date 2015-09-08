package org.corespring.platform.core.services.item

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ ObjectMetadata, DeleteObjectsRequest, S3Object }
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }
import play.api.Logger

import scalaz.{ Validation }

class SupportingMaterialsAssets[A](s3: AmazonS3, bucket: String, assetKeys: AssetKeys[A]) {

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

  def upload(id: A, resource: Resource, file: StoredFile, bytes: Array[Byte]): Validation[String, StoredFile] = Validation.fromTryCatch {
    logger.debug(s"[upload] id=$id, resource=${resource.name}, file=${file.name}")
    val key = assetKeys.supportingMaterialFile(id, resource.name, file.name)
    val metadata = new ObjectMetadata()
    metadata.setContentType(file.contentType)
    metadata.setContentLength(bytes.length.toLong)
    s3.putObject(bucket, key, new ByteArrayInputStream(bytes), metadata)
    file
  }.leftMap(_.getMessage)

  def deleteFile(id: A, resource: Resource, filename: String): Validation[String, String] = Validation.fromTryCatch {
    logger.debug(s"[deleteFile] id=$id, resource=${resource.name}, file=${filename}")
    val key = assetKeys.supportingMaterialFile(id, resource.name, filename)
    s3.deleteObject(bucket, key)
    filename
  }.leftMap { _.getMessage }
}
