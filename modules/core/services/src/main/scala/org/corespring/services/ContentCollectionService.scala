package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

trait ContentCollectionService {

  def insertCollection(orgId: ObjectId, coll: ContentCollection, p: Permission, enabled: Boolean = true): Either[PlatformServiceError, ContentCollection]

  def updateCollection(coll: ContentCollection): Either[PlatformServiceError, ContentCollection]

  def delete(collId: ObjectId): Validation[PlatformServiceError, Unit]

  def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef]

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId]

  def getCollections(orgId: ObjectId, p: Permission): Either[PlatformServiceError, Seq[ContentCollection]]

  def getPublicCollections: Seq[ContentCollection]

  def isPublic(collectionId: ObjectId): Boolean

  /** How many items are associated with this collectionId */
  def itemCount(collectionId: ObjectId): Long

  /** Get a default collection from the set of ids */
  def getDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection]

  /**
   *
   * @param orgs contains a sequence of (organization id -> permission) tuples
   * @param collId
   * @return
   */
  def addOrganizations(orgs: Seq[(ObjectId, Permission)], collId: ObjectId): Either[PlatformServiceError, Unit]

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
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * Share the items returned by the query with the specified collection.
   *
   * @param orgId
   * @param query
   * @param collId
   * @return
   */
  def shareItemsMatchingQuery(orgId: ObjectId, query: String, collId: ObjectId): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Either[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * does the given organization have access to the given collection with given permissions?
   * @param orgId
   * @param collId
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean

}
