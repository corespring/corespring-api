package org.corespring.api.v2.services

trait PermissionResult {
  def granted: Boolean

  def reasons: Seq[String]
}

case object Granted extends PermissionResult {
  override def granted: Boolean = true

  override def reasons: Seq[String] = Seq.empty
}

case class Denied(reasons: String*) extends PermissionResult {
  override def granted: Boolean = false
}

trait PermissionService[CLIENT, DATA] {
  def create(client: CLIENT, newValue: DATA): PermissionResult
  def get(client: CLIENT, value: DATA): PermissionResult
}
