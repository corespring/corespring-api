package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.User

object SimpleUser {
  def fromUser(u: User): SimpleUser = {
    SimpleUser(u.id, u.userName, u.provider, u.fullName)
  }
}

case class SimpleUser(id: ObjectId, userName: String, provider: String, fullName: String)
