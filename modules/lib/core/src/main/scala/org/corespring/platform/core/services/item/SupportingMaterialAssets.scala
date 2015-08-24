package org.corespring.platform.core.services.item

import com.amazonaws.services.s3.model.S3Object
import org.corespring.platform.core.models.item.resource.{ StoredFile, Resource }

import scalaz.Validation

trait SupportingMaterialAssets[ID] {
  def deleteDir(id: ID, resource: Resource): Validation[String, Resource]

  def deleteFile(id: ID, resource: Resource, name: String): Validation[String, String]

  def upload(id: ID, resource: Resource, file: StoredFile, bytes: Array[Byte]): Validation[String, StoredFile]

  def getS3Object(id: ID, materialName: String, file: String, etag: Option[String]): Option[S3Object]
}
