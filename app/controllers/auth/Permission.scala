package controllers.auth

/**
 * A Permission
 */
case class Permission(value: Long, name: String)
{
  Permission.add(this)
}

object Permission {
  private var byValue = Map.empty[Long, Permission]
  private var byName = Map.empty[String, Permission]

  val CreateUser = new Permission(1, "create_user")
  val PostContent = new Permission(1 << 1, "post_content")
  val AddCollection = new Permission(1 << 2, "add_collection")
  val RemoveCollection = new Permission(1 << 3, "remove_collection")
  val GetSubOrganizations = new Permission(1 << 4, "get_sub_organizations")
  val AssignCollection = new Permission(1 << 5, "assign_collection") //only for libraries
  val PostSubOrganization = new Permission(1 << 6, "post_sub_organization")
  val GetLibraryOrganizations = new Permission(1 << 7, "get_library_organizations")

  val All = new Permission(Long.MaxValue, "all")
  val None = new Permission(0,"none")

  private def add(p: Permission) {
    byValue += (p.value -> p)
    byName +=  (p.name -> p)
  }

  def fromLong(value: Long): Option[Permission] =  byValue.get(value)
  def fromString(value: String): Option[Permission] = byName.get(value)
}