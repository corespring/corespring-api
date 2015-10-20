package org.corespring.services

import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, Organization, ContentCollRef, ContentCollection }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

case class ContentCollectionUpdate(name: Option[String], isPublic: Option[Boolean])
case class OrgAccess(orgId: ObjectId, permission: Permission)

trait ContentCollectionService {

  /** Enable this collection for this org */
  def enableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /** Enable the collection for the org */
  def disableCollectionForOrg(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  def shareCollectionWithOrg(collectionId: ObjectId, orgId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef]

  def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection]

  def listCollectionsByOrg(orgId: ObjectId): Stream[ContentCollection]

  /**
   * List all collections that the given orgId has access to.
   * @param orgId
   * @return a stream of [[CollectionInfo]]
   */
  def listAllCollectionsAvailableForOrg(orgId: ObjectId): Stream[CollectionInfo]

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

  def getContentCollRefs(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ContentCollRef]

  def getCollectionIds(orgId: ObjectId, p: Permission, deep: Boolean = true): Seq[ObjectId]

  def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]]

  def getPublicCollections: Seq[ContentCollection]

  def isPublic(collectionId: ObjectId): Boolean

  /** How many items are associated with this collectionId */
  def itemCount(collectionId: ObjectId): Long

  /** Get a default collection from the set of ids */
  def getDefaultCollection(collections: Seq[ObjectId]): Option[ContentCollection]

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
  def shareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  /**
   * Unshare the specified items from the specified collections
   *
   * @param orgId
   * @param items - sequence of items to be unshared from
   * @param collIds - sequence of collections to have the items removed from
   * @return
   */
  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collIds: Seq[ObjectId]): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]]

  def unShareItems(orgId: ObjectId, items: Seq[VersionedId[ObjectId]], collId: ObjectId): Validation[PlatformServiceError, Seq[VersionedId[ObjectId]]] = {
    unShareItems(orgId, items, Seq(collId))
  }

  /**
   * does the given organization have access to the given collection with given permission.
   * Aka - can org 'a' 'write' to collection 'c'?
   * @param orgId
   * @param collId
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, Unit]

  /**
   * does the given organization have access to all the given collections with given permissions?
   * @param orgId
   * @param collIds
   */
  def isAuthorized(orgId: ObjectId, collIds: Seq[ObjectId], p: Permission): Validation[PlatformServiceError, Unit]

  /**
   * Is the item shared by the collection
   * @param itemId
   * @param collId
   * @return
   */
  def isItemSharedWith(itemId: VersionedId[ObjectId], collId: ObjectId): Boolean
}
