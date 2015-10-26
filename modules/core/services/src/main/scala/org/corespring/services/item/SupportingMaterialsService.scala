package org.corespring.services.item

import org.corespring.models.item.resource.{StoredFileDataStream, BaseFile, Resource}

import scalaz.Validation

trait SupportingMaterialsService[ID] {
  def create(id: ID, resource: Resource, bytes: => Array[Byte]): Validation[String, Resource]
  def delete(id: ID, materialName: String): Validation[String, Seq[Resource]]
  def removeFile(id: ID, materialName: String, filename: String): Validation[String, Resource]
  def addFile(id: ID, materialName: String, file: BaseFile, bytes: => Array[Byte]): Validation[String, Resource]
  def getFile(id: ID, materialName: String, file: String, etag: Option[String] = None): Validation[String, StoredFileDataStream]
  def updateFileContent(id: ID, materialName: String, file: String, content: String): Validation[String, Resource]
}
