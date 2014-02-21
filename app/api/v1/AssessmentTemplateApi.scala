package api.v1

import play.api.libs.json.Json._
import controllers.auth.BaseApi
import org.corespring.platform.core.services.assessment.template._
import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.bson.types.ObjectId
import scala.Some
import play.api.mvc.{AnyContent, Action, Result}
import play.api.libs.json.Json
import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject

class AssessmentTemplateApi(assessmentTemplateService: AssessmentTemplateService)
  extends ContentApi[AssessmentTemplate] {

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

  def list(query: Option[String], fields: Option[String], count: String, skip: Int, limit: Int,
           sort: Option[String]): Action[AnyContent] = ApiAction { request =>
    val templates = assessmentTemplateService.find().toSeq.map(_.toAssessmentTemplate)
    count match {
      case "true" => Ok(Json.obj("count" -> templates.length))
      case _ => Ok(Json.toJson(templates))
    }
  }

  def listAndCount(query: Option[String], fields: Option[String], skip: Int, limit: Int,
                   sort: Option[String]): Action[AnyContent] = ApiAction { request =>
    val templates = assessmentTemplateService.find().toSeq.map(_.toAssessmentTemplate)
    Ok(Json.obj(
      "count" -> templates.length,
      "data" -> Json.toJson(templates)
    ))
  }
}

object AssessmentTemplateApi extends AssessmentTemplateApi(AssessmentTemplateServiceImpl)