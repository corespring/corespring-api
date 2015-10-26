package org.corespring.servicesAsync

import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.{ Organization, ContentCollection }

import scalaz.Validation
import scala.concurrent.Future

case class ContentCollectionUpdate(name: Option[String], isPublic: Option[Boolean])

trait ContentCollectionService {

  def create(name: String, org: Organization): Future[Validation[PlatformServiceError, ContentCollection]]

  /**
   * Insert the new collection such that, the owner org has write access to it.
   * @param coll
   * @return
   */
  def insertCollection(coll: ContentCollection): Future[Validation[PlatformServiceError, ContentCollection]]

  def archiveCollectionId: Future[ObjectId]

  def findOneById(id: ObjectId): Future[Option[ContentCollection]]

  def update(id: ObjectId, update: ContentCollectionUpdate): Future[Validation[PlatformServiceError, ContentCollection]]

  /**
   * delete the collection
   * fails if the itemCount for the collection > 0
   * @param collId
   * @return
   */
  def delete(collId: ObjectId): Future[Validation[PlatformServiceError, Unit]]

  def getPublicCollections: Future[Seq[ContentCollection]]

  def isPublic(collectionId: ObjectId): Future[Boolean]

}
