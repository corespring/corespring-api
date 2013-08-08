package org.corespring.platform.core.models.auth

/**
 * A Permission
 */
case class Permission(value: Long, name: String) {
  def has(p: Permission):Boolean = {
    (value&p.value) == p.value
  }
}

object Permission {

  val None = new Permission(0, "none")
  val Read = new Permission(1, "read")
  val Write = new Permission(3, "write") // write is 3 instead of two (11 instead of 10) because read permission is implied in write

  def fromLong(value: Long): Option[Permission] = value match {
    case 0 => Some(None)
    case x if((x&Write.value)==Write.value) => Some(Write)
    case x if((x&Read.value)==Read.value) => Some(Read)
    case _ => scala.None
  }

  def fromString(value: String): Option[Permission] = value match {
    case "none" => Some(None)
    case "read" => Some(Read)
    case "write" => Some(Write)
    case _ => scala.None
  }

  def toHumanReadable(l:Long) : String = fromLong(l).map(_.name).getOrElse("Unknown Permission")
}