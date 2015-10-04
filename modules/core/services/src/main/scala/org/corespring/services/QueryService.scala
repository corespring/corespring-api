package org.corespring.services

import com.mongodb.DBObject

trait Query {
  def term: String
}

trait QueryService[A, Q <: Query] {

  @deprecated("use 'query' instead", "core-refactor")
  def find(dbo: DBObject): Stream[A]

  /**
   * Search for term in A.
   * @param term
   * @return
   */
  def query(term: Q, l: Int = 50, sk: Int = 0): Stream[A]

  @deprecated("use 'query(Query) instead", "core-refactor")
  def query(term: String): Stream[A]

  def list(): Stream[A]

  def findOne(id: String): Option[A]
}
