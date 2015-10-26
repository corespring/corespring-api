package org.corespring.servicesAsync.item

import org.corespring.models.item.resource.{ StoredFileDataStream, BaseFile, Resource }

import scalaz.Validation
import scala.concurrent.Future

trait SupportingMaterialsService[ID] {
  def create(id: ID, resource: Resource, bytes: => Array[Byte]): Future[Validation[String, Resource]]
  def delete(id: ID, materialName: String): Future[Validation[String, Seq[Resource]]]
  def removeFile(id: ID, materialName: String, filename: String): Future[Validation[String, Resource]]
  def addFile(id: ID, materialName: String, file: BaseFile, bytes: => Array[Byte]): Future[Validation[String, Resource]]
  def getFile(id: ID, materialName: String, file: String, etag: Option[String] = None): Future[Validation[String, StoredFileDataStream]]
  def updateFileContent(id: ID, materialName: String, file: String, content: String): Future[Validation[String, Resource]]
}
