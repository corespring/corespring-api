package org.corespring.api.v1

import com.mongodb.casbah.Imports
import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatMongoCursor
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.item.{ Content, Item }
import org.corespring.platform.data.mongo.SalatVersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.BaseContentService

import scala.concurrent.Future
import scalaz.Validation

trait SalatContentService[ContentType <: Content[ID], ID] extends BaseContentService[ContentType, ID] {

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[ContentType]
}

class ItemApiContentService(underlying: BaseContentService[Item, VersionedId[ObjectId]], dao: SalatVersioningDao[Item])
  extends SalatContentService[Item, VersionedId[ObjectId]] {
  override def find(query: Imports.DBObject, fields: Imports.DBObject): SalatMongoCursor[Item] = {
    dao.findCurrent(query, fields)
  }

  override def insert(i: Item): Option[VersionedId[ObjectId]] = underlying.insert(i)

  override def isAuthorized(orgId: Imports.ObjectId, contentId: VersionedId[Imports.ObjectId], p: Permission): Validation[PlatformServiceError, Unit] = {
    underlying.isAuthorized(orgId, contentId, p)
  }

  override def isAuthorizedBatch(orgId: ObjectId, idAndPermissions: (VersionedId[ObjectId], Permission)*): Future[Seq[((VersionedId[ObjectId], Permission), Boolean)]] = {
    underlying.isAuthorizedBatch(orgId, idAndPermissions: _*)
  }

  override def clone(content: Item): Validation[String, Item] = {
    underlying.clone(content)
  }

  override def findOneById(id: VersionedId[ObjectId]): Option[Item] = {
    underlying.findOneById(id)
  }

  override def save(i: Item, createNewVersion: Boolean): Validation[PlatformServiceError, VersionedId[ObjectId]] = {
    underlying.save(i, createNewVersion)
  }

}
