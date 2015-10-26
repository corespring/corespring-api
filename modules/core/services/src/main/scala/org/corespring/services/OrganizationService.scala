package org.corespring.services

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ MetadataSetRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.Validation

trait OrganizationService {

  def save(o: Organization): Validation[PlatformServiceError, Organization]

  def list(sk: Int = 0, l: Int = 0): Stream[Organization]

  //TODO: Move to MetadataSetService
  def addMetadataSet(orgId: ObjectId, setId: ObjectId): Validation[String, MetadataSetRef]

  def orgsWithPath(orgId: ObjectId, deep: Boolean): Stream[Organization]
  /**
   * remove metadata set by id
   * @param orgId
   * @param setId
   * @return maybe an error string
   */
  //TODO: Move to MetadataSetService
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
  def getTree(parentId: ObjectId): Stream[Organization]
}
