package org.corespring.services.salat.assessment

import com.mongodb.{ BasicDBObject, DBObject }
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.Context
import com.novus.salat.dao.SalatDAO
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate
import org.corespring.services.salat.HasDao
import org.joda.time.DateTime
import org.corespring.{ services => interface }

class AssessmentTemplateService(
  val dao: SalatDAO[AssessmentTemplate, ObjectId],
  val context: Context)
  extends interface.assessment.AssessmentTemplateService
  with HasDao[AssessmentTemplate, ObjectId] {

  //TODO Do we need to check against ("data.name" -> "template") ?
  private def baseAssessmentTemplateQuery: DBObject = MongoDBObject.empty

  private def baseFind(query: DBObject, fields: DBObject = new BasicDBObject(), limit: Int = 0, skip: Int = 0): Stream[AssessmentTemplate] = {
    dao.find(new MongoDBObject(baseAssessmentTemplateQuery) ++ query, fields).limit(limit).skip(skip).toStream
  }

  override def all: Stream[AssessmentTemplate] = find(baseAssessmentTemplateQuery)

  override def count(query: DBObject = new BasicDBObject()): Int = dao.count(query).toInt

  override def create(assessmentTemplate: AssessmentTemplate) = dao.insert(assessmentTemplate)

  override def find(query: DBObject, fields: DBObject = new BasicDBObject(), limit: Int = 0, skip: Int = 0): Stream[AssessmentTemplate] = {
    baseFind(query, fields, limit,skip)
  }

  override def findOneById(id: ObjectId): Option[AssessmentTemplate] = dao.findOneById(id)

  override def findOneByIdAndOrg(id: ObjectId, orgId: ObjectId): Option[AssessmentTemplate] =
    dao.findOne(MongoDBObject("_id" -> id, "orgId" -> orgId))

  override def insert(assessmentTemplate: AssessmentTemplate): Option[ObjectId] = dao.insert(assessmentTemplate)

  override def findWithIds(ids: Seq[ObjectId]): Stream[AssessmentTemplate] =
    baseFind(MongoDBObject("_id" -> MongoDBObject("$in" -> ids)))

  override def save(assessmentTemplate: AssessmentTemplate) = {
    val result = dao.save(assessmentTemplate.copy(dateModified = Some(new DateTime())))
    if (result.getLastError.ok) {
      Right(assessmentTemplate.id)
    } else {
      Left(result.getLastError.getErrorMessage)
    }
  }
}

