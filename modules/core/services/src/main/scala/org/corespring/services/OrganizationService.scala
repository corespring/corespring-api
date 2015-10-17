package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, MetadataSetRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.PlatformServiceError

import scalaz.Validation

//TODO: Add new Service [[OrgCollectionsService]] and thin out [[OrganizationService]] and [[ContentCollectionService]]
trait OrganizationService {

  def list(sk: Int = 0, l: Int = 0): Stream[Organization]

  def getOrgsWithAccessTo(collectionId: ObjectId): Stream[Organization]

  def getOrgPermissionForItem(orgId: ObjectId, itemId: VersionedId[ObjectId]): Option[Permission]

  def getOrCreateDefaultCollection(orgId: ObjectId): Validation[PlatformServiceError, ContentCollection]

  def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization]

  def addMetadataSet(orgId: ObjectId, setId: ObjectId): Validation[String, MetadataSetRef]

  def deleteCollectionFromAllOrganizations(collId: ObjectId): Validation[String, Unit]

  def addCollectionReference(orgId: ObjectId, reference: ContentCollRef): Validation[PlatformServiceError, Unit]

  /**
   * Add the public collection to all orgs to that they have access to it
   * @param collectionId
   * @return
   */
  def addPublicCollectionToAllOrgs(collectionId: ObjectId): Validation[PlatformServiceError, Unit]

  /**
   * remove metadata set by id
   * @param orgId
   * @param setId
   * @return maybe an error string
   */
  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Validation[PlatformServiceError, MetadataSetRef]

  def findOneById(orgId: ObjectId): Option[Organization]

  def findOneByName(name: String): Option[Organization]

  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Validation[PlatformServiceError, Organization]

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Validation[PlatformServiceError, Unit]

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  @deprecated("legacy function for v1 api - remove once v1 is gone", "core-refactor")
  def getTree(parentId: ObjectId): Seq[Organization]

  def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean

  def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean

  def removeCollection(orgId: ObjectId, collId: ObjectId): Validation[PlatformServiceError, Unit]

  def getPermissions(orgId: ObjectId, collId: ObjectId): Option[Permission]

  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Validation[PlatformServiceError, ContentCollRef]

  /** Enable this collection for this org */
  def enableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

  /** Enable the collection for the org */
  def disableCollection(orgId: ObjectId, collectionId: ObjectId): Validation[PlatformServiceError, ContentCollRef]

}
