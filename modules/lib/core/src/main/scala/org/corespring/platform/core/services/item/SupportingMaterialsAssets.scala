package org.corespring.platform.core.services.item

import java.io.ByteArrayInputStream

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ ObjectMetadata, DeleteObjectsRequest, S3Object }
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }

import scalaz.{ Validation }

class SupportingMaterialsAssets[A](s3: AmazonS3, bucket: String, assetKeys: AssetKeys[A]) {

  def deleteDir(id: A, resource: Resource): Validation[String, Resource] = Validation.fromTryCatch {
    val listing = s3.listObjects(assetKeys.supportingMaterialFolder(id, resource.name))
    import scala.collection.JavaConversions._
    val keys: List[String] = listing.getObjectSummaries.toList.map { s =>
      s.getKey
    }
    val parent = assetKeys.supportingMaterialFolder(id, resource.name)
    val allKeys: List[String] = keys :+ parent
    val request = new DeleteObjectsRequest(bucket).withKeys(allKeys: _*)
    s3.deleteObjects(request)
    resource
  }.leftMap(_.getMessage)

  def getS3Object(id: A, materialName: String, file: String, etag: Option[String]): Option[S3Object] = {
    val key = assetKeys.supportingMaterialFile(id, materialName, file)
    Some(s3.getObject(bucket, key))
  }

  def upload(id: A, resource: Resource, file: StoredFile, bytes: Array[Byte]): Validation[String, StoredFile] = Validation.fromTryCatch {
    val key = assetKeys.supportingMaterialFile(id, resource.name, file.name)
    val metadata = new ObjectMetadata()
    metadata.setContentType(file.contentType)
    metadata.setContentLength(bytes.length.toLong)
    s3.putObject(bucket, key, new ByteArrayInputStream(bytes), metadata)
    file
  }.leftMap(_.getMessage)

  def deleteFile(id: A, resource: Resource, name: String): Validation[String, String] = Validation.fromTryCatch {
    val key = assetKeys.supportingMaterialFile(id, resource.name, name)
    s3.deleteObject(bucket, key)
    name
  }.leftMap { _.getMessage }
}
