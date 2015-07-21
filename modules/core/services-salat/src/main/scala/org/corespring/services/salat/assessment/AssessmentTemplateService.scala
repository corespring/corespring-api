package org.corespring.services.salat.assessment

import com.mongodb.{ BasicDBObject, DBObject }
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate
import org.corespring.services.salat.HasDao
import org.joda.time.DateTime
import org.corespring.{ services => interface }

trait AssessmentTemplateService
    extends interface.assessment.AssessmentTemplateService
    with HasDao[AssessmentTemplate, ObjectId] {

  private def baseAssessmentTemplateQuery: DBObject = MongoDBObject("data.name" -> "template")

  override def create(assessmentTemplate: AssessmentTemplate) = dao.insert(assessmentTemplate)

  override def clone(content: AssessmentTemplate): Option[AssessmentTemplate] = ???

  override def count(query: DBObject): Int = dao.count(query).toInt

  override def all: Stream[AssessmentTemplate] = find(baseAssessmentTemplateQuery)

  override def find(query: DBObject, fields: DBObject = new BasicDBObject(), limit: Int = 0, skip: Int = 0): Stream[AssessmentTemplate] = {
    dao.find(new MongoDBObject(baseAssessmentTemplateQuery) ++ query, fields).limit(limit).skip(skip).toStream
  }

  override def findOneById(id: ObjectId): Option[AssessmentTemplate] = dao.findOneById(id)

  override def save(assessmentTemplate: AssessmentTemplate) = {
    val result = dao.save(assessmentTemplate.copy(dateModified = Some(new DateTime())))
    if (result.getLastError.ok) {
      Right(assessmentTemplate.id)
    } else {
      Left(result.getLastError.getErrorMessage)
    }
  }

  override def insert(assessmentTemplate: AssessmentTemplate): Option[ObjectId] = dao.insert(assessmentTemplate)

  override def findWithIds(ids: Seq[ObjectId]): Stream[AssessmentTemplate] =
    this.find(MongoDBObject("_id" -> MongoDBObject("$in" -> ids)))

}

