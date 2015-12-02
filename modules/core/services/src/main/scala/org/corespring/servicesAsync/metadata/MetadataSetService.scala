package org.corespring.servicesAsync.metadata

import org.bson.types.ObjectId
import org.corespring.models.metadata.MetadataSet

import scalaz.Validation
import scala.concurrent.Future

trait MetadataSetService {
  def update(set: MetadataSet): Future[Validation[String, MetadataSet]]

  def create(orgId: ObjectId, set: MetadataSet): Future[Validation[String, MetadataSet]]

  def delete(orgId: ObjectId, setId: ObjectId): Future[Option[String]]

  def list(orgId: ObjectId): Future[Seq[MetadataSet]]

  def findByKey(key: String): Future[Option[MetadataSet]]

  def findOneById(id: ObjectId): Future[Option[MetadataSet]]
}
