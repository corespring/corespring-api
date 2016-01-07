package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.item.{Item, Passage}
import org.corespring.platform.data.mongo.models.VersionedId

import scala.concurrent.Future

trait PassageService {

  def get(id: VersionedId[ObjectId]): Future[Option[Passage]]
  def insert(i: Passage): Future[Option[VersionedId[ObjectId]]]

}
