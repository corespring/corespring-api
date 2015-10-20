package org.corespring.services.item

import org.bson.types.ObjectId
import org.corespring.models.item.FieldValue
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait FieldValueService {

  def get: Option[FieldValue]

  def insert(f: FieldValue): Validation[PlatformServiceError, ObjectId]

  def delete(id: ObjectId): Validation[PlatformServiceError, ObjectId]

}
