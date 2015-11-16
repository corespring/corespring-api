package org.corespring.platform.core.controllers.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.{ User, Organization }

/**
 * A class that holds authorization information for an API call.  This is created in the BaseApi trait.
 */
case class AuthorizationContext(val organization: ObjectId, val user: Option[String] = None, val isSSLogin: Boolean = false, val org: Option[Organization] = None) {
  lazy val permission: Permission = user match {
    case Some(username) => User.getPermissions(username, organization) match {
      case Right(p) => p
      case Left(e) => Permission.None
    }
    case None => Permission.Write
  }
  lazy val isLoggedIn: Boolean = isSSLogin
}