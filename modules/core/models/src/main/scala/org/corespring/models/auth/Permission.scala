package org.corespring.models.auth

case class Permission(value: Long, name: String) {
  def has(p: Permission): Boolean = {
    (value & p.value) == p.value
  }
}

object Permission {

  val Read = new Permission(1, "read")
  val Write = new Permission(3, "write") // write is 3 instead of two (11 instead of 10) because read permission is implied in write

  def fromLong(value: Long): Option[Permission] = value match {
    case 0 => None
    case x if ((x & Write.value) == Write.value) => Some(Write)
    case x if ((x & Read.value) == Read.value) => Some(Read)
    case _ => None
  }

  def fromString(value: String): Option[Permission] = value match {
    case "none" => None
    case "read" => Some(Read)
    case "write" => Some(Write)
    case _ => None
  }

  def toHumanReadable(l: Long): String = fromLong(l).map(_.name).getOrElse("Unknown Permission")
}