package org.corespring.servicesAsync

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

  def queryDotNotation(dotNotation: String, l: Int = 50, sk: Int = 0): Future[Stream[Standard]]

  def delete(id: ObjectId): Future[Boolean]

  def insert(standard: Standard): Future[Option[ObjectId]]

  def count(query: DBObject): Future[Long]

  def findOneById(id: ObjectId): Future[Option[Standard]]

  def findOneByDotNotation(dotNotation: String): Future[Option[Standard]]

  def domains: Future[StandardDomains]
}
