package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation


trait Auth[D,IDENTITY,UID] {
  def loadForRead(id: String)(implicit identity: IDENTITY): Validation[V2Error, D]
  def loadForWrite(id: String)(implicit identity: IDENTITY): Validation[V2Error, D]
  def save(data: D, createNewVersion: Boolean)(implicit identity: IDENTITY)
  def insert(data: D)(implicit identity: IDENTITY): Option[UID]
}

trait ItemAuth[A] extends Auth[Item, A, VersionedId[ObjectId]]{
  def canCreateInCollection(collectionId: String)(implicit identity: A): Validation[V2Error, Boolean]
}


