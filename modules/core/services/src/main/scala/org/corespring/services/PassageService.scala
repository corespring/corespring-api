package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.item.{Item, Passage}
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future
import scalaz.Validation

trait PassageService {

  def get(id: VersionedId[ObjectId]): Future[Validation[PlatformServiceError, Option[Passage]]]
  def insert(passage: Passage): Future[Validation[PlatformServiceError, Passage]]
  def save(passage: Passage): Future[Validation[PlatformServiceError, Passage]]

}
