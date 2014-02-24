package api.v1

import play.api.libs.json.Json._
import org.corespring.platform.core.services.assessment.template._
import org.corespring.platform.core.models.assessment.{SalatAssessmentTemplate, AssessmentTemplate}
import org.bson.types.ObjectId
import scala.Some
import play.api.mvc.Result
import play.api.libs.json.Json


class AssessmentTemplateApi(assessmentTemplateService: AssessmentTemplateService)
  extends ContentApi[SalatAssessmentTemplate](assessmentTemplateService)(AssessmentTemplate.ContentViewWrites) {

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  def WithAssessmentTemplate(id: ObjectId, orgId: ObjectId)(f: AssessmentTemplate => Result): Result =
    assessmentTemplateService.findOneById(id) match {
      case Some(q) => {
        q.orgId match {
          case Some(assessmentOrgId) => {
            if (assessmentOrgId == orgId) f(q.toAssessmentTemplate) else BadRequest("You can't access this assessment")
          }
          case _ => NotFound
        }
      }
      case _ => NotFound
    }

  def create() = ApiAction {
    request =>
      parsed[AssessmentTemplate](request.body.asJson, {
        assessmentTemplate =>
          val copy = assessmentTemplate.copy(orgId = Some(request.ctx.organization))
          assessmentTemplateService.save(copy.forSalat)
          Ok(toJson(copy))
      })
  }

  def update(id: ObjectId) = ApiAction { request =>
    WithAssessmentTemplate(id, request.ctx.organization) { dbTemplate =>
      parsed[AssessmentTemplate](request.body.asJson, { requestTemplate =>
        val newTemplate = dbTemplate.copy(
          questions = if (requestTemplate.questions.length > 0) requestTemplate.questions else dbTemplate.questions,
          metadata = (dbTemplate.metadata ++ requestTemplate.metadata)
        )
        assessmentTemplateService.save(newTemplate.forSalat)
        Ok(Json.toJson(newTemplate))
      })
    }
  }

  def contentType: String = AssessmentTemplate.contentType

}

object AssessmentTemplateApi
  extends AssessmentTemplateApi(AssessmentTemplateServiceImpl)