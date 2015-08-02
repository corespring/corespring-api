package org.corespring.services

import com.mongodb.DBObject

trait QueryService[A] {

  def find(dbo: DBObject): Stream[A]

  def query(term: String): Stream[A]

  def list(): Stream[A]

  def findOne(id: String): Option[A]
}
