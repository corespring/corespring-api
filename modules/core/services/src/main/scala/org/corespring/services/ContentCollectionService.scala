package org.corespring.services

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.{ Organization, ContentCollection }

import scalaz.Validation

case class ContentCollectionUpdate(name: Option[String], isPublic: Option[Boolean])

trait ContentCollectionService {

  def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection]

  /**
   * Insert the new collection such that, the owner org has write access to it.
   * @param coll
   * @return
   */
  def insertCollection(coll: ContentCollection): Validation[PlatformServiceError, ContentCollection]

  def archiveCollectionId: ObjectId

  def findOneById(id: ObjectId): Option[ContentCollection]

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

}
