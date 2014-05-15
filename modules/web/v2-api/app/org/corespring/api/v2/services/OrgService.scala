package org.corespring.api.v2.services

import org.corespring.platform.core.models.Organization
import org.bson.types.ObjectId

trait OrgService {

  def defaultCollection(o: Organization): Option[ObjectId]
}
