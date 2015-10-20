package org.corespring.models

import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

case class User(userName: String = "",
  fullName: String = "",
  email: String = "",
  lastLoginDate: Option[DateTime] = None,
  registrationDate: Option[DateTime] = None,
  org: UserOrg,
  password: String = "",
  provider: String = "userpass",
  id: ObjectId = new ObjectId())


case class UserOrg(orgId: ObjectId, pval: Long)

