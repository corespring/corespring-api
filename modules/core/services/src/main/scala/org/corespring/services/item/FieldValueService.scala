package org.corespring.services.item

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.FieldValue

import scalaz.Validation

trait FieldValueService {

  def get: Option[FieldValue]

  def insert(f: FieldValue): Validation[PlatformServiceError, ObjectId]

  def delete(id: ObjectId): Validation[PlatformServiceError, ObjectId]

}
