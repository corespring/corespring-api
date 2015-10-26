package org.corespring.servicesAsync

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.Subject
import scala.concurrent.Future

case class SubjectQuery(term: String, subject: Option[String], category: Option[String]) extends Query

trait SubjectService extends QueryService[Subject, SubjectQuery] {

  def delete(id: ObjectId): Future[Boolean]

  def insert(s: Subject): Future[Option[ObjectId]]

  def count(query: DBObject): Future[Long]

  def findOneById(id: ObjectId): Future[Option[Subject]]
}
