package org.corespring.platform.core.services.assessment.template

import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.data.mongo.SalatVersioningDao
import play.api.{PlayException, Application}
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat.SalatPlugin
import com.mongodb.casbah
import com.novus.salat.Context

trait AssessmentTemplateService {
  def create(assessmentTemplate: AssessmentTemplate)
}

class AssessmentTemplateServiceImpl(dao: SalatVersioningDao[AssessmentTemplate]) extends AssessmentTemplateService {
  def create(assessmentTemplate: AssessmentTemplate) = dao.insert(assessmentTemplate)
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
