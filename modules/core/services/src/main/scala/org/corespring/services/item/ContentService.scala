package org.corespring.services.item

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.item.Content
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait BaseFindAndSaveService[ContentType <: Content[ID], ID] {

  def save(i: ContentType, createNewVersion: Boolean = false): Validation[PlatformServiceError, ID]

  def findOneById(id: ID): Option[ContentType]
}

//trait Cursor[T]{
//  def count : Int
//  def sort(dbo:DBObject) : Cursor[T]
//  def skip(count:Int) : Cursor[T]
//  def limit(count:Int) : Cursor[T]
//}
trait BaseContentService[ContentType <: Content[ID], ID] extends BaseFindAndSaveService[ContentType, ID] {

  //def find[T](query: DBObject, fields: DBObject = new BasicDBObject()): Cursor[T]

  def clone(content: ContentType): Option[ContentType]

  def insert(i: ContentType): Option[ID]

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Validation[PlatformServiceError, Unit]

}
