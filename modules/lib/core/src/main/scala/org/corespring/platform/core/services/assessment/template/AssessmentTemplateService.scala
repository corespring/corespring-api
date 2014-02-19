package org.corespring.platform.core.services.assessment.template

import org.corespring.platform.core.models.assessment.SalatAssessmentTemplate
import org.corespring.platform.data.mongo.SalatVersioningDao
import play.api.{PlayException, Application}
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah
import com.novus.salat.Context
import org.corespring.platform.core.services.BaseContentService
import org.corespring.platform.data.mongo.models.VersionedId
import com.novus.salat.dao.SalatMongoCursor
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime

trait AssessmentTemplateService extends BaseContentService[SalatAssessmentTemplate, VersionedId[ObjectId]]

class AssessmentTemplateServiceImpl(dao: SalatVersioningDao[SalatAssessmentTemplate]) extends AssessmentTemplateService {

  def create(assessmentTemplate: SalatAssessmentTemplate) = dao.insert(assessmentTemplate)

  def clone(content: SalatAssessmentTemplate): Option[SalatAssessmentTemplate] = ???

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  def count(query: DBObject, fields: Option[String] = None): Int = dao.countCurrent(query).toInt

  def find(query: DBObject, fields: DBObject): SalatMongoCursor[SalatAssessmentTemplate] = dao.findCurrent(query, fields)

  def findOneById(id: VersionedId[ObjectId]): Option[SalatAssessmentTemplate] = dao.findOneById(id)

  def findOne(query: DBObject): Option[SalatAssessmentTemplate] = dao.findOneCurrent(query)

  def save(assessmentTemplate: SalatAssessmentTemplate, createNewVersion: Boolean) =
    dao.save(assessmentTemplate.copy(dateModified = Some(new DateTime())), createNewVersion)

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean) =  dao.update(id, dbo, createNewVersion)

  def insert(assessmentTemplate: SalatAssessmentTemplate): Option[VersionedId[ObjectId]] = dao.insert(assessmentTemplate)

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[SalatAssessmentTemplate] =
    dao.findCurrent(MongoDBObject("_id._id" -> MongoDBObject("$in" -> ids.map(i => i.id))), keys).toSeq

}

object AssessmentTemplateVersioningDao extends SalatVersioningDao[SalatAssessmentTemplate] {

  import play.api.Play.current

  private def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  protected def db: casbah.MongoDB = salatDb()
  protected def collectionName: String = "content"
  protected implicit def entityManifest: Manifest[SalatAssessmentTemplate] = Manifest.classType(classOf[SalatAssessmentTemplate])
  protected implicit def context: Context = org.corespring.platform.core.models.mongoContext.context

}

object AssessmentTemplateServiceImpl extends AssessmentTemplateServiceImpl(AssessmentTemplateVersioningDao)
