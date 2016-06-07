package org.corespring.services
import com.mongodb.casbah.Imports._
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }

import scala.concurrent.Future
import scalaz.Validation

case class CollectionIdPermission(collectionId: ObjectId, permission: Permission)

/**
 * A service describing the relationship between an [[Organization]] and a [[ContentCollection]].
 */
trait OrgCollectionService {
  /**
   * does the given organization have access to the given collection with given permission.
   * Aka - can org 'a' 'write' to collection 'c'?
   */
  def isAuthorized(orgId: ObjectId, collectionId: ObjectId, p: Permission): Boolean
  def isAuthorized(orgId: ObjectId, collectionIdAndPermission: CollectionIdPermission): Boolean

  /**
   * A batch api for checking multiple authorizations at once.
   * does the given organization have access to the given collection with given permission.
   *
   * @param orgId
   * @param collectionIdAndPermission
   * @return
   */
  def isAuthorizedBatch(orgId: ObjectId, collectionIdAndPermission: CollectionIdPermission*): Future[Seq[(CollectionIdPermission, Boolean)]]

  def getPermission(orgId: ObjectId, collectionId: ObjectId): Option[Permission]

  def getPermissions(orgId: ObjectId, collectionIds: ObjectId*): Future[Seq[(ObjectId, Option[Permission])]]

  def ownsCollection(org: Organization, collectionId: ObjectId): Validation[PlatformServiceError, Boolean]

  def getCollections(orgId: ObjectId, p: Permission): Validation[PlatformServiceError, Seq[ContentCollection]]

  /**
   * List all collections that the given orgId has access to.
   *
   * @param orgId
   * @return a stream of [[org.corespring.models.CollectionInfo]]
   */
  def listAllCollectionsAvailableForOrg(orgId: ObjectId, skip: Int, limit: Int): Future[Stream[CollectionInfo]]

  def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization]

  /**
   * Get the default collection for this org, create if necessary.
   *
   * @param orgId
   * @return
   */
  def getDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection]

  /**
   * Give the given orgId the permission for the given collectionId.
   * If a permission already exists, update it.
   * If a permission is disabled enable it
   *
   * @return
   */
  def grantAccessToCollection(orgId: ObjectId, collectionId: ObjectId, p: Permission): Validation[PlatformServiceError, Organization]

  /**
   * Remove the given orgId's access to the given collectionId.
   *
   * @param orgId
   * @param collectionId
   * @return
   */
  def removeAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, Organization]

  /**
   * remove all access to this collection, including the owner's access
   * //TODO: Check if removing owner's access is correct
   *
   * @return
   */
  def removeAllAccessToCollection(collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  /**
   * Enable this org's access to this collection.
   * If access hasn't been granted do nothing.
   */
  def enableOrgAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /**
   * Disable this org's access to this collection
   * If access hasn't been granted do nothing.
   */
  def disableOrgAccessToCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

}
