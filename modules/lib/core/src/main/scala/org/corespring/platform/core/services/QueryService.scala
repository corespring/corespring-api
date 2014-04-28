package org.corespring.platform.core.services

trait QueryService[A] {
  def query(term: String): Seq[A]

  def list(): Seq[A]

  def findOne(id: String): Option[A]
}