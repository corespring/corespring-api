package org.corespring.platform.core.controllers.auth

import org.bson.types.ObjectId
import org.corespring.models.Organization
import org.corespring.models.auth.Permission

/**
 * A class that holds authorization information for an API call.  This is created in the BaseApi trait.
 */
case class AuthorizationContext(orgId: ObjectId,
  user: Option[String] = None,
  org: Option[Organization] = None,
  permission: Permission,
  isLoggedInUser: Boolean) {
}