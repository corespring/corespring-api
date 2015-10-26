package org.corespring.servicesAsync

import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ MetadataSetRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId

import scalaz.Validation
import scala.concurrent.Future

trait OrganizationService {

  def save(o: Organization): Future[Validation[PlatformServiceError, Organization]]

  def list(sk: Int = 0, l: Int = 0): Future[Stream[Organization]]

  //TODO: Move to MetadataSetService
  def addMetadataSet(orgId: ObjectId, setId: ObjectId): Future[Validation[String, MetadataSetRef]]

  def orgsWithPath(orgId: ObjectId, deep: Boolean): Future[Stream[Organization]]
  /**
   * remove metadata set by id
   * @param orgId
   * @param setId
   * @return maybe an error string
   */
  //TODO: Move to MetadataSetService
  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Future[Validation[PlatformServiceError, MetadataSetRef]]

  def findOneById(orgId: ObjectId): Future[Option[Organization]]

  def findOneByName(name: String): Future[Option[Organization]]

  /**
   * insert organization. if parent exists, insert as child of parent, otherwise, insert as root of new nested set tree
   * @param org - the organization to be inserted
   * @param optParentId - the parent of the organization to be inserted or none if the organization is to be root of new tree
   * @return - the organization if successfully inserted, otherwise none
   */
  def insert(org: Organization, optParentId: Option[ObjectId]): Future[Validation[PlatformServiceError, Organization]]

  /**
   * delete the specified organization and all sub-organizations
   * @param orgId
   * @return
   */
  def delete(orgId: ObjectId): Future[Validation[PlatformServiceError, Unit]]

  /**
   * get all sub-nodes of given organization.
   * if none, or parent could not be found in database, returns empty list
   * @param parentId
   * @return
   */
  @deprecated("legacy function for v1 api - remove once v1 is gone", "core-refactor")
  def getTree(parentId: ObjectId): Future[Stream[Organization]]
}
