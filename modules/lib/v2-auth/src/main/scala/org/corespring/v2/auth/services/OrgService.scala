package org.corespring.v2.auth.services

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.{ ContentCollection, Organization }

trait OrgService {
  def defaultCollection(o: Organization): Option[ObjectId]
  def defaultCollection(oid: ObjectId): Option[ObjectId]
  def org(id: ObjectId): Option[Organization]
}
