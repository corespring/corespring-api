package org.corespring.platform.core.services.assessment.template

import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.data.mongo.SalatVersioningDao
import play.api.{PlayException, Application}
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah
import com.novus.salat.Context
import org.corespring.platform.core.services.BaseContentService
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.Imports
import com.novus.salat.dao.SalatMongoCursor
import com.mongodb.casbah.commons.MongoDBObject
import org.joda.time.DateTime

trait AssessmentTemplateService extends BaseContentService[AssessmentTemplate, VersionedId[ObjectId]]

class AssessmentTemplateServiceImpl(dao: SalatVersioningDao[AssessmentTemplate]) extends AssessmentTemplateService {
  
  def create(assessmentTemplate: AssessmentTemplate) = dao.insert(assessmentTemplate)

  def clone(content: AssessmentTemplate): Option[AssessmentTemplate] = ???

  def findFieldsById(id: VersionedId[ObjectId], fields: DBObject = MongoDBObject.empty): Option[DBObject] = dao.findDbo(id, fields)

  def count(query: DBObject, fields: Option[String] = None): Int = dao.countCurrent(query).toInt

  def find(query: DBObject, fields: DBObject): SalatMongoCursor[AssessmentTemplate] = dao.findCurrent(query, fields)

  def findOneById(id: VersionedId[ObjectId]): Option[AssessmentTemplate] = dao.findOneById(id)

  def findOne(query: DBObject): Option[AssessmentTemplate] = dao.findOneCurrent(query)

  def save(assessmentTemplate: AssessmentTemplate, createNewVersion: Boolean) =
    dao.save(assessmentTemplate.copy(dateModified = Some(new DateTime())), createNewVersion)

  def saveUsingDbo(id: VersionedId[ObjectId], dbo: DBObject, createNewVersion: Boolean) =  dao.update(id, dbo, createNewVersion)

  def insert(assessmentTemplate: AssessmentTemplate): Option[VersionedId[ObjectId]] = dao.insert(assessmentTemplate)

  def findMultiple(ids: Seq[VersionedId[ObjectId]], keys: DBObject): Seq[AssessmentTemplate] =
    dao.findCurrent(MongoDBObject("_id._id" -> MongoDBObject("$in" -> ids.map(i => i.id))), keys).toSeq

}

object AssessmentTemplateVersioningDao extends SalatVersioningDao[AssessmentTemplate] {

  import play.api.Play.current

  private def salatDb(sourceName: String = "default")(implicit app: Application): MongoDB = {
    app.plugin[SalatPlugin].map(_.db(sourceName)).getOrElse(throw new PlayException("SalatPlugin is not " +
      "registered.", "You need to register the plugin with \"500:se.radley.plugin.salat.SalatPlugin\" in conf/play.plugins"))
  }

  protected def db: casbah.MongoDB = salatDb()
  protected def collectionName: String = "content"
  protected implicit def entityManifest: Manifest[AssessmentTemplate] = Manifest.classType(classOf[AssessmentTemplate])
  protected implicit def context: Context = org.corespring.platform.core.models.mongoContext.context

}

object AssessmentTemplateServiceImpl extends AssessmentTemplateServiceImpl(AssessmentTemplateVersioningDao)
