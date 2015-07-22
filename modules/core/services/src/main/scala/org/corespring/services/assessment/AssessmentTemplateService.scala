package org.corespring.services.assessment

import com.mongodb.{ DBObject }
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate

trait AssessmentTemplateService {
  def find(query: DBObject, fields: DBObject, limit: Int = 0, skip: Int = 0): Stream[AssessmentTemplate]
  def findOne(query: DBObject, fields: DBObject): Option[AssessmentTemplate]
  def findWithIds(ids: Seq[ObjectId]): Stream[AssessmentTemplate]
  def all: Stream[AssessmentTemplate]
  def create(assessmentTemplate: AssessmentTemplate)
  def count(query: DBObject): Int
  def clone(content: AssessmentTemplate): Option[AssessmentTemplate]
  def findOneById(id: ObjectId): Option[AssessmentTemplate]
  def save(assessmentTemplate: AssessmentTemplate): Either[String, ObjectId]
  def insert(assessmentTemplate: AssessmentTemplate): Option[ObjectId]
}
