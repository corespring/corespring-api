package org.corespring.services

import com.mongodb.casbah.commons.MongoDBObject

trait QueryService[A] {

  def find(dbo: MongoDBObject): Stream[A]

  def query(term: String): Stream[A]

  def list(): Stream[A]

  def findOne(id: String): Option[A]
}
