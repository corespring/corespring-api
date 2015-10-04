package org.corespring.services

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.models.Subject

case class SubjectQuery(term: String, subject: Option[String], category: Option[String]) extends Query

trait SubjectService extends QueryService[Subject, SubjectQuery] {

  def delete(id: ObjectId): Boolean

  def insert(s: Subject): Option[ObjectId]

  def count(query: DBObject): Long

  def findOneById(id: ObjectId): Option[Subject]
}
