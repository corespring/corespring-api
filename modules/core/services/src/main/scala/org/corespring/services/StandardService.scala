package org.corespring.services

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.{ StandardDomains, Standard }

import scala.concurrent.Future

case class StandardQuery(term: String,
  standard: Option[String],
  subject: Option[String],
  category: Option[String],
  subCategory: Option[String]) extends Query

trait StandardService extends QueryService[Standard, StandardQuery] {

  def count(query: DBObject): Long
  def delete(id: ObjectId): Boolean
  def domains: Future[StandardDomains]
  def findOneByDotNotation(dotNotation: String): Option[Standard]
  def findOneById(id: ObjectId): Option[Standard]
  def insert(standard: Standard): Option[ObjectId]
  def queryDotNotation(dotNotation: String, l: Int = 50, sk: Int = 0): Stream[Standard]
}
