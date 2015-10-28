package org.corespring.services.assessment

import com.mongodb.{BasicDBObject, DBObject}
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate

trait AssessmentTemplateService {
  def all: Stream[AssessmentTemplate]
  def count(query: DBObject = new BasicDBObject()): Int
  def create(assessmentTemplate: AssessmentTemplate): Unit
  def find(query: DBObject, fields: DBObject, limit: Int = 0, skip: Int = 0): Stream[AssessmentTemplate]
  def findOneById(id: ObjectId): Option[AssessmentTemplate]
  def findOneByIdAndOrg(id: ObjectId, orgId:ObjectId): Option[AssessmentTemplate]
  def findWithIds(ids: Seq[ObjectId]): Stream[AssessmentTemplate]
  def insert(assessmentTemplate: AssessmentTemplate): Option[ObjectId]
  def save(assessmentTemplate: AssessmentTemplate): Either[String, ObjectId]
}
