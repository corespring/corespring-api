package org.corespring.services

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.{ Organization, ContentCollection }
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

case class ContentCollectionUpdate(name: Option[String], isPublic: Option[Boolean])

trait ContentCollectionService {

  def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection]

  def archiveCollectionId: ObjectId

  def findByDbo(dbo: DBObject, fields: Option[DBObject] = None, sort: Option[DBObject] = None, skip: Int = 0, limit: Int = 0): Stream[ContentCollection]

  def count(dbo: DBObject): Long

  def findOneById(id: ObjectId): Option[ContentCollection]

  def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission, enabled: Boolean = true): Validation[PlatformServiceError, ContentCollection]

  def update(id: ObjectId, update: ContentCollectionUpdate): Validation[PlatformServiceError, ContentCollection]

  /**
   * delete the collection
   * fails if the itemCount for the collection > 0
   * @param collId
   * @return
   */
  def delete(collId: ObjectId): Validation[PlatformServiceError, Unit]

  def getPublicCollections: Seq[ContentCollection]

  def isPublic(collectionId: ObjectId): Boolean

  /** How many items are associated with this collectionId */
  def itemCount(collectionId: ObjectId): Long

  /** Get a default collection from the set of ids */
  def getDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection]

}
