package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.models.{Organization, User}

object SimpleUser {
  def fromUser(u: User): SimpleUser = {
    SimpleUser(u.id, u.userName, u.provider, u.fullName, u.org.orgId)
  }
}

case class SimpleUser(id: ObjectId, userName: String, provider: String, fullName: String, orgId: ObjectId)

case class SimpleOrg(id: ObjectId, name: String)

object SimpleOrg {
  def fromOrganization(o: Organization): SimpleOrg = SimpleOrg(o.id, o.name)
}

case class OrgAndUser(val org: SimpleOrg, val user: Option[SimpleUser] = None)
