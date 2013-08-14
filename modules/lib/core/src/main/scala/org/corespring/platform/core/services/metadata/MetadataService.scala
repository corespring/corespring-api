package org.corespring.platform.core.services.metadata

import org.corespring.platform.core.models.metadata.MetadataSet
import org.bson.types.ObjectId


trait MetadataService {
  def update(set: MetadataSet): Either[String, MetadataSet]

  def create(orgId: ObjectId, set: MetadataSet): Either[String, MetadataSet]

  def delete(orgId: ObjectId, setId: ObjectId): Option[String]

  def list(orgId: ObjectId): Seq[MetadataSet]

  def findByKey(key: String): Option[MetadataSet]

  def findOneById(id: ObjectId): Option[MetadataSet]
}