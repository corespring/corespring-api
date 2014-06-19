package org.corespring.v2player.integration.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemAuth {
  def canCreateInCollection(collectionId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def loadForRead(itemId: String)(implicit header: RequestHeader): Validation[String, Item]
  def loadForWrite(itemId: String)(implicit header: RequestHeader): Validation[String, Item]

  def save(item: Item, createNewVersion: Boolean)(implicit header: RequestHeader)
  def insert(item: Item)(implicit header: RequestHeader): Option[VersionedId[ObjectId]]
}
