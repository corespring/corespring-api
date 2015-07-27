package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, MetadataSetRef, Organization }
import org.corespring.services.errors.PlatformServiceError

trait OrganizationService {

  def defaultCollection(o: Organization): Option[ObjectId]
  def defaultCollection(oid: ObjectId): Option[ObjectId]

  def orgsWithPath(orgId: ObjectId, deep: Boolean): Seq[Organization]

  def addMetadataSet(orgId: ObjectId, setId: ObjectId, checkExistence: Boolean = true): Either[String, MetadataSetRef]

  def deleteCollectionFromAllOrganizations(collId: ObjectId): Either[String, Unit]

  def addCollectionReference(orgId: ObjectId, reference: ContentCollRef): Either[PlatformServiceError, Unit]

  /**
   * Add the public collection to all orgs to that they have access to it
   * @param collectionId
   * @return
   */
  def addPublicCollectionToAllOrgs(collectionId: ObjectId): Either[PlatformServiceError, Unit]

  /**
   * remove metadata set by id
   * @param orgId
   * @param setId
   * @return maybe an error string
   */
  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Either[PlatformServiceError, MetadataSetRef]

  def findOneById(orgId: ObjectId): Option[Organization]

  def findOneByName(name: String): Option[Organization]

  def getDefaultCollection(orgId: ObjectId): Either[PlatformServiceError, ContentCollection]

  //def isRoot(org:Organization) : Boolean

  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Either[PlatformServiceError, Organization]

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Either[PlatformServiceError, Unit]

  @deprecated("use changeName instead", "0.0.1")
  def updateOrganization(org: Organization): Either[PlatformServiceError, Organization]
  def changeName(orgId: ObjectId, name: String): Either[PlatformServiceError, ObjectId]

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  def getTree(parentId: ObjectId): Seq[Organization]

  def isChild(parentId: ObjectId, childId: ObjectId): Boolean

  def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean

  def canAccessCollection(org: Organization, collectionId: ObjectId, permission: Permission): Boolean

  def hasCollRef(orgId: ObjectId, collRef: ContentCollRef): Boolean

  def removeCollection(orgId: ObjectId, collId: ObjectId): Either[PlatformServiceError, Unit]

  def getPermissions(orgId: ObjectId, collId: ObjectId): Permission

  def addCollection(orgId: ObjectId, collId: ObjectId, p: Permission): Either[PlatformServiceError, ContentCollRef]

  def setCollectionEnabledStatus(orgId: ObjectId, collectionId: ObjectId, enabledState: Boolean): Either[PlatformServiceError, ContentCollRef]

  def updateCollection(orgId: ObjectId, collRef: ContentCollRef): Either[PlatformServiceError, ContentCollRef]

}
