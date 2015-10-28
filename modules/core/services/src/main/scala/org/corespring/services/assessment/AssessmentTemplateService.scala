package org.corespring.services.assessment

import com.mongodb.{ BasicDBObject, DBObject }
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate

trait AssessmentTemplateService {
  def findByOrg(orgId: ObjectId): Stream[AssessmentTemplate]
  def findOneById(id: ObjectId): Option[AssessmentTemplate]
  def findOneByIdAndOrg(id: ObjectId, orgId: ObjectId): Option[AssessmentTemplate]
  def insert(assessmentTemplate: AssessmentTemplate): Option[ObjectId]
  def save(assessmentTemplate: AssessmentTemplate): Either[String, ObjectId]
}
