package org.corespring.v2.auth.services

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization

trait OrgService {

  def defaultCollection(o: Organization): Option[ObjectId]
  def org(id:ObjectId) : Option[Organization]
}
