package controllers.auth

/**
 * A Permission
 */
case class Permission(value: Long, name: String) {
  def has(p: Permission):Boolean = {
    (value&p.value) == p.value
  }
}

object Permission {
//  val CreateUser = new Permission(1, "create_user")
//  val PostContent = new Permission(1 << 1, "post_content")
//  val AddCollection = new Permission(1 << 2, "add_collection")
//  val RemoveCollection = new Permission(1 << 3, "remove_collection")
//  val GetSubOrganizations = new Permission(1 << 4, "get_sub_organizations")
//  val AssignCollection = new Permission(1 << 5, "assign_collection")
//  val PostSubOrganization = new Permission(1 << 6, "post_sub_organization")


//  val All = new Permission(Long.MaxValue, "all")
  val None = new Permission(0, "none")
  val Read = new Permission(1, "read")
  val Write = new Permission(3, "write") // write is 3 instead of two (11 instead of 10) because read permission is implied in write

  def fromLong(value: Long): Option[Permission] = value match {
    case 0 => Some(None)
    case x if((x&Read.value)==Read.value) => Some(Read)
    case x if((x&Write.value)==Write.value) => Some(Write)
    case _ => scala.None
  }

  def fromString(value: String): Option[Permission] = value match {
    case "none" => Some(None)
    case "read" => Some(Read)
    case "write" => Some(Write)
    case _ => scala.None
  }
}