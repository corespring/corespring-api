package controllers.auth

import org.bson.types.ObjectId

/**
 * A class that holds authorization information for an API call.  This is created in the BaseApi trait.
 *
 * @see BaseApi
 */
class AuthorizationContext(val organization: ObjectId, val user: Option[String] = None) {
  private val permissions = new scala.collection.mutable.HashMap[ObjectId, PermissionSet]

  /**
   * Checks whether the passed Permission is granted over the target
   *
   * @param target - the organization id
   * @param requestedPermission - one of the permissions defined in Permission
   * @return true if the permission is granted or false otherwise
   */
  def hasPermission(target: ObjectId, requestedPermission: Permission): Boolean = {
    if ( target == organization && user.isEmpty ) {
      // if the context does not have a user and the action is targeted at the same organization the caller belongs to then
      // this is an admin call -> grant anything
      true
    } else {
      // check if we have a PermissionSet for the target
      val permissionSet = permissions.getOrElse(target, {
        // we don't have a set, create one by querying the DB
        val set = new PermissionSet
        // todo: review permissions
        //set.grant(Permission.fromLong(AuthService.hasPermission(organization, target, user, requestedPermission)).getOrElse(Permission.None))
        // cache it
        permissions.put(target, set)
        set
      })
      permissionSet.has(requestedPermission)
    }
  }

  /**
   * Checks whether the passed Permission is granted over the caller organization
   *
   * @param requestedPermission - one of the permissions defined in Permission
   * @return true if the permission is granted or false otherwise
   */
  def hasPermission(requestedPermission: Permission): Boolean = hasPermission(organization, requestedPermission)

  /**
   * Adds a permission set for a target
   *
   * @param target - the organization id
   * @param set - a permission set
   */
  def addPermissionSet(target: ObjectId, set: PermissionSet) {
    permissions.put(target, set)
  }

  def getPermissionSet(target: ObjectId): Option[PermissionSet] = permissions.get(target)

  /**
   * Removes permissions for a target
   *
   * @param target - the organization id
   */
  def removePermissionSet(target: ObjectId) {
    permissions.remove(target)
  }

  /**
   * Removes all permissions in the context.
   */
  def clearPermissions() {
    permissions.clear()
  }
}
