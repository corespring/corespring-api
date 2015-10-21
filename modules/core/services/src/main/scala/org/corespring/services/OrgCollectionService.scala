package org.corespring.services
import com.mongodb.casbah.Imports._
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

/**
 * A service describing the relationship between an [[Organization]] and a [[ContentCollection]].
 */
trait OrgCollectionService {
  /**
   * does the given organization have access to the given collection with given permission.
   * Aka - can org 'a' 'write' to collection 'c'?
   */
  def isAuthorized(orgId: ObjectId, collId: ObjectId, p: Permission): Boolean

  def getPermission(orgId: ObjectId, collId: ObjectId): Option[Permission]

  def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Boolean]

  /** Enable this collection for this org */
  def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /** Enable the collection for the org */
  def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]]

  /**
   * List all collections that the given orgId has access to.
   * @param orgId
   * @return a stream of [[org.corespring.models.CollectionInfo]]
   */
  def listAllCollectionsAvailableForOrg(orgId: ObjectId): Stream[CollectionInfo]

  def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization]

  /**
   * Get the default collection for this org, create if necessary.
   * @param orgId
   * @return
   */
  def getDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection]

  def removeAccessToCollectionForAllOrgs(collId: ObjectId): Validation[PlatformServiceError, Unit]

  /**
   * Give the given orgId the permission for the given collectionId.
   * If a permission already exists, update it.
   * @return
   */
  def grantAccessToCollection(orgId: ObjectId, collectionId: ObjectId, p: Permission): Validation[PlatformServiceError, Organization]
  def removeAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, Organization]

}
