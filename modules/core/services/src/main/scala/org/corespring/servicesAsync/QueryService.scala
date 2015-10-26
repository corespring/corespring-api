package org.corespring.servicesAsync

import scala.concurrent.Future

trait Query {
  def term: String
}

trait QueryService[A, Q <: Query] {

  /**
   * Search for term in A.
   * @param term
   * @return
   */
  def query(term: Q, l: Int = 50, sk: Int = 0): Future[Stream[A]]

  def list(l: Int = 0, sk: Int = 0): Future[Stream[A]]

  def findOne(id: String): Future[Option[A]]
}
