package org.corespring.servicesAsync.item

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.FieldValue

import scalaz.Validation
import scala.concurrent.Future

trait FieldValueService {

  def get: Future[Option[FieldValue]]

  def insert(f: FieldValue): Future[Validation[PlatformServiceError, ObjectId]]

  def delete(id: ObjectId): Future[Validation[PlatformServiceError, ObjectId]]

}
