package org.corespring.platform.core.services.assessment.template

import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import org.corespring.platform.core.services.BaseContentService
import com.novus.salat.dao.{ModelCompanion, SalatDAO, SalatMongoCursor}
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime
import org.corespring.platform.core.models.assessment.SalatAssessmentTemplate
import scala.Some
import com.mongodb.casbah.commons.TypeImports.ObjectId
import play.api.Play.current
import org.corespring.platform.core.models.mongoContext.context

trait AssessmentTemplateService extends BaseContentService[SalatAssessmentTemplate, ObjectId] {
  def find(): SalatMongoCursor[SalatAssessmentTemplate]
}

class AssessmentTemplateServiceImpl(salatDao: SalatDAO[SalatAssessmentTemplate, ObjectId])
  extends AssessmentTemplateService {

  private val baseAssessmentTemplateQuery = MongoDBObject("data.name" -> "template")

  private object Dao extends ModelCompanion[SalatAssessmentTemplate, ObjectId] {
    val dao = salatDao
  }

  def create(assessmentTemplate: SalatAssessmentTemplate) = salatDao.insert(assessmentTemplate)

  def clone(content: SalatAssessmentTemplate): Option[SalatAssessmentTemplate] = ???

  def count(query: DBObject, fields: Option[String] = None): Int = salatDao.count(query).toInt

  def find(): SalatMongoCursor[SalatAssessmentTemplate] = find(baseAssessmentTemplateQuery)

  def find(query: DBObject, fields: DBObject = new BasicDBObject()): SalatMongoCursor[SalatAssessmentTemplate] = {
    salatDao.find(baseAssessmentTemplateQuery ++ query, fields)
  }

  def findOneById(id: ObjectId): Option[SalatAssessmentTemplate] = salatDao.findOneById(id)

  def findOne(query: DBObject): Option[SalatAssessmentTemplate] = salatDao.findOne(query)

  def save(assessmentTemplate: SalatAssessmentTemplate, createNewVersion: Boolean) =
    Dao.save(assessmentTemplate.copy(dateModified = Some(new DateTime())))

  def insert(assessmentTemplate: SalatAssessmentTemplate): Option[ObjectId] = salatDao.insert(assessmentTemplate)

  def findMultiple(ids: Seq[ObjectId], keys: DBObject): Seq[SalatAssessmentTemplate] =
    salatDao.find(MongoDBObject("_id" -> MongoDBObject("$in" -> ids)), keys).toSeq

}

object AssessmentTemplateDao
  extends SalatDAO[SalatAssessmentTemplate, ObjectId](collection = mongoCollection("content"))

object AssessmentTemplateServiceImpl extends AssessmentTemplateServiceImpl(AssessmentTemplateDao)
