package org.corespring.servicesAsync.item

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.item.Content
import org.corespring.platform.data.mongo.models.VersionedId
import scala.concurrent.Future

import scalaz.Validation

trait BaseFindAndSaveService[ContentType <: Content[ID], ID] {

  def save(i: ContentType, createNewVersion: Boolean = false): Future[Validation[PlatformServiceError, ID]]

  def findOneById(id: ID): Future[Option[ContentType]]
}

//trait Cursor[T]{
//  def count  : Future[Int]
//  def sort(dbo:DBObject)  : Future[Cursor[T]]
//  def skip(count:Int)  : Future[Cursor[T]]
//  def limit(count:Int)  : Future[Cursor[T]]
//}
trait BaseContentService[ContentType <: Content[ID], ID] extends BaseFindAndSaveService[ContentType, ID] {

  //def find[T](query: DBObject, fields: DBObject = new BasicDBObject())  : Future[Cursor[T]]

  def clone(content: ContentType): Future[Option[ContentType]]

  def insert(i: ContentType): Future[Option[ID]]

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Future[Validation[PlatformServiceError, Unit]]

}
