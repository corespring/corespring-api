package org.corespring.services

trait QueryService[A] {
  def query(term: String): Stream[A]

  def list(): Stream[A]

  def findOne(id: String): Option[A]
}
