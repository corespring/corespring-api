package org.corespring.platform.core.services

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.error.CorespringInternalError
import org.corespring.models.{ ContentCollection, MetadataSetRef, Organization }
import org.corespring.platform.core.services.metadata.MetadataSetServiceImpl

trait ContentCollectionService {

  def getCollections(orgId: ObjectId, permission: Permission): Either[CorespringInternalError, Seq[ContentCollection]]

}
