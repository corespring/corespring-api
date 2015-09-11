package org.corespring.services.metadata

import org.bson.types.ObjectId
import org.corespring.models.metadata.MetadataSet

import scalaz.Validation

trait MetadataSetService {
  def update(set: MetadataSet): Validation[String, MetadataSet]

  def create(orgId: ObjectId, set: MetadataSet): Validation[String, MetadataSet]

  def delete(orgId: ObjectId, setId: ObjectId): Option[String]

  def list(orgId: ObjectId): Seq[MetadataSet]

  def findByKey(key: String): Option[MetadataSet]

  def findOneById(id: ObjectId): Option[MetadataSet]
}
