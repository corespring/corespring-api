package org.corespring.platform.core.controllers.auth

import org.corespring.models.Organization
import org.corespring.models.auth.Permission

/**
 * A class that holds authorization information for an API call.  This is created in the BaseApi trait.
 */
case class AuthorizationContext(
  user: Option[String] = None,
  org: Organization,
  permission: Permission,
  isLoggedInUser: Boolean) {

  def orgId = org.id
}