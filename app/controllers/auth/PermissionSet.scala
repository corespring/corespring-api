package controllers.auth

/**
 * A Permission Set
 */

class PermissionSet {
  private var permissions: Long = 0

  /**
   * Adds a permission to the set
   * @param pvars a Permission
   */
  def grant(pvars: Permission*) {
    for (p <- pvars)
      permissions = permissions | p.value
  }

  /**
   * Removes a Permission from the set
   * @param pvars a Permission
   */
  def revoke(pvars: Permission*) {
    for (p <- pvars)
      permissions = permissions ^ p.value
  }

  /**
   * Checks whether the set has a Permission included or not
   * @param p a Permission
   * @return returns true if the set has the permission of false otherwise
   */
  def has(p: Permission): Boolean = {
    (permissions & p.value) == p.value
  }
}


