package api.v1

import play.api.libs.json.Json._
import scala.Some
import controllers.auth.BaseApi
import org.corespring.platform.core.services.assessment.template._
import org.corespring.platform.core.models.assessment.AssessmentTemplate

class AssessmentTemplateApi(assessmentTemplateService: AssessmentTemplateService) extends BaseApi {

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  def create() = ApiAction {
    request =>
      parsed[AssessmentTemplate](request.body.asJson, {
        assessmentTemplate =>
          val copy = assessmentTemplate.copy(orgId = Some(request.ctx.organization))
          assessmentTemplateService.save(copy)
          Ok(toJson(copy))
      })
  }

}

object AssessmentTemplateApi extends AssessmentTemplateApi(AssessmentTemplateServiceImpl)