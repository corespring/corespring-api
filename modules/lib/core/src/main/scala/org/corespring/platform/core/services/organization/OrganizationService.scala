package org.corespring.platform.core.services.organization

import org.bson.types.ObjectId
import org.corespring.platform.core.services.metadata.MetadataSetServiceImpl
import org.corespring.platform.core.models.{ ContentCollection, Organization, MetadataSetRef }
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.CorespringInternalError

trait OrganizationService {

  def metadataSetService: MetadataSetServiceImpl

  def addMetadataSet(orgId: ObjectId, setId: ObjectId, checkExistence: Boolean = true): Either[String, MetadataSetRef]

  /**
   * remove metadata set by id
   * @param orgId
   * @param setId
   * @return maybe an error string
   */
  def removeMetadataSet(orgId: ObjectId, setId: ObjectId): Option[String]

  def findOneById(orgId: ObjectId): Option[Organization]

  def canAccessCollection(orgId: ObjectId, collectionId: ObjectId, permission: Permission): Boolean

  def getDefaultCollection(orgId: ObjectId): Either[CorespringInternalError, ContentCollection]
}
