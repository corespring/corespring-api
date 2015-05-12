package org.corespring.platform.core.services

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.CorespringInternalError
import org.corespring.platform.core.models.{ContentCollection, MetadataSetRef, Organization}
import org.corespring.platform.core.services.metadata.MetadataSetServiceImpl

trait ContentCollectionService {

  def getCollections(orgId: ObjectId, permission: Permission): Either[CorespringInternalError, Seq[ContentCollection]]

}
