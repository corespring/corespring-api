package org.corespring.servicesAsync.assessment

import com.mongodb.{ DBObject }
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate
import scala.concurrent.Future

trait AssessmentTemplateService {
  def find(query: DBObject, fields: DBObject, limit: Int = 0, skip: Int = 0): Future[Stream[AssessmentTemplate]]
  def findOne(query: DBObject, fields: DBObject): Future[Option[AssessmentTemplate]]
  def findWithIds(ids: Seq[ObjectId]): Future[Stream[AssessmentTemplate]]
  def all: Future[Stream[AssessmentTemplate]]
  def create(assessmentTemplate: AssessmentTemplate): Unit
  def count(query: DBObject): Future[Int]
  def clone(content: AssessmentTemplate): Future[Option[AssessmentTemplate]]
  def findOneById(id: ObjectId): Future[Option[AssessmentTemplate]]
  def save(assessmentTemplate: AssessmentTemplate): Future[Either[String, ObjectId]]
  def insert(assessmentTemplate: AssessmentTemplate): Future[Option[ObjectId]]
}
