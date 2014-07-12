package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemAuth {
  def canCreateInCollection(collectionId: String)(implicit header: RequestHeader): Validation[V2Error, Boolean]
  def loadForRead(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Item]
  def loadForWrite(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Item]

  def save(item: Item, createNewVersion: Boolean)(implicit header: RequestHeader)
  def insert(item: Item)(implicit header: RequestHeader): Option[VersionedId[ObjectId]]
}
