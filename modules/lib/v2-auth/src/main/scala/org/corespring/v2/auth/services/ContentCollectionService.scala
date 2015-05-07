package org.corespring.v2.auth.services

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.{ContentCollection, Organization}

trait ContentCollectionService {
  def getCollections(o: Organization, p: Permission): Seq[ContentCollection]
}
