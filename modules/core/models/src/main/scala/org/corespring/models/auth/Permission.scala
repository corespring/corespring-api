package org.corespring.models.auth

case class Permission(value: Long, name: String, read: Boolean = false, write: Boolean = false, canClone: Boolean = false) {
  def has(p: Permission): Boolean = p match {
    case Permission.Write => this.write
    case Permission.Read => this.read
    case Permission.Clone => this.canClone
  }
}

object Permission {

  val Read = new Permission(1, "read", read = true)
  val Clone = new Permission(5, "clone", read = true, canClone = true)
  val Write = new Permission(3, "write", read = true, canClone = true, write = true)

  def fromLong(value: Long): Option[Permission] = value match {
    case 0 => None
    case 1 => Some(Read)
    case 3 => Some(Write)
    case 5 => Some(Clone)
    case _ => None
  }

  def fromString(value: String): Option[Permission] = value match {
    case "none" => None
    case "read" => Some(Read)
    case "write" => Some(Write)
    case "clone" => Some(Clone)
    case _ => None
  }

  def toHumanReadable(l: Long): String = fromLong(l).map(_.name).getOrElse("Unknown Permission")
}