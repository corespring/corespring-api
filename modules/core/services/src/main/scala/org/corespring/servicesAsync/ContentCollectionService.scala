package org.corespring.servicesAsync

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scala.concurrent.Future
import scalaz.Validation

trait ContentCollectionService {

  def archiveCollectionId: Future[ObjectId]

  def findByDbo(dbo: DBObject, fields: Option[DBObject] = None, sort: Option[DBObject] = None, skip: Int = 0, limit: Int = 0): Future[Stream[ContentCollection]]

  def count(dbo: DBObject): Future[Long]

  def findOneById(id: ObjectId): Future[Option[ContentCollection]]

  def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission, enabled: Boolean = true): Future[Either[PlatformServiceError, ContentCollection]]

  def updateCollection(coll: ContentCollection): Future[Either[PlatformServiceError, ContentCollection]]

  def delete(collId: ObjectId): Future[Validation[PlatformServiceError, Unit]]

  def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Future[Seq[ContentCollRef]]

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Future[Seq[ObjectId]]

  def getCollections(orgId: ObjectId, p: Permission): Either[PlatformServiceError, Future[Seq[ContentCollection]]]

  def getPublicCollections: Future[Seq[ContentCollection]]

  def isPublic(collectionId: ObjectId): Future[Boolean]

  /** How many items are associated with this collectionId */
  def itemCount(collectionId: ObjectId): Future[Long]

  /** Get a default collection from the set of ids */
  def getDefaultCollection(collections: Seq[ObjectId]): Future[Option[ContentCollection]]

  /**
   *
   * @param orgs contains a sequence of (organization id -> permission) tuples
   * @param collId
   * @return
   */
  def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Future[Either[PlatformServiceError, Unit]]

  /**
   * Share items to the collection specified.
   * - must ensure that the context org has write access to the collection
   * - must ensure that the context org has read access to the items being added
   *
   * @param orgId
   * @param items
   * @param collId
   * @return
   */
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Future[Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]]

  /**
   * Share the items returned by the query with the specified collection.
   *
   * @param orgId
   * @param query
   * @param collId
   * @return
   */
  def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Future[Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]]

  /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Future[Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]]

  /**
   * does the given organization have access to the given collection with given permissions?
   * @param orgId
   * @param collId
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Future[Boolean]

}
