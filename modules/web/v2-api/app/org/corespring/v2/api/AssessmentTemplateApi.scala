package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate
import org.corespring.models.json.JsonFormatting
import org.corespring.services.assessment.AssessmentTemplateService
import org.corespring.v2.actions.{ OrgRequest, V2Actions }
import org.corespring.v2.errors.Errors.{ cantFindAssessmentTemplateWithId, incorrectJsonFormat }
import play.api.libs.json.{ JsError, JsObject, JsSuccess, Json }
import play.api.mvc.{ AnyContent, Request }

import scala.concurrent.ExecutionContext

class AssessmentTemplateApi(
  actions: V2Actions,
  assessmentTemplateService: AssessmentTemplateService,
  jsonFormatting: JsonFormatting,
  v2ApiContext: V2ApiExecutionContext)
  extends V2Api {

  override def ec: ExecutionContext = v2ApiContext.context

  implicit val AssessmentTemplateFormat = jsonFormatting.formatAssessmentTemplate

  def get = actions.Org { request: OrgRequest[AnyContent] =>
    val t = assessmentTemplateService.findByOrg(request.org.id)
    Ok(Json.toJson(t))
  }

  def getById(assessmentTemplateId: ObjectId) = actions.Org { request: OrgRequest[AnyContent] =>
    val t = assessmentTemplateService.findOneByIdAndOrg(assessmentTemplateId, request.org.id)

    t match {
      case Some(dbResult) => Ok(Json.toJson(t))
      case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
    }
  }

  def create() = actions.Org { request: OrgRequest[AnyContent] =>
    val json = getJson(request.org.id, request)
    Json.fromJson[AssessmentTemplate](json) match {
      case JsSuccess(jsonAssessment, _) => {
        val assessmentTemplate = new AssessmentTemplate().merge(jsonAssessment)
        assessmentTemplateService.insert(assessmentTemplate)
        Created(Json.toJson(assessmentTemplate))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def update(assessmentTemplateId: ObjectId) = actions.Org { request: OrgRequest[AnyContent] =>
    val json = getJson(request.org.id, request)
    Json.fromJson[AssessmentTemplate](json) match {
      case JsSuccess(jsonAssessment, _) => {
        //TODO: RF: Low-Priority: could $set or update work here?
        assessmentTemplateService.findOneByIdAndOrg(assessmentTemplateId, request.org.id) match {
          case Some(dbResult) => {
            val updatedAssessment = dbResult.merge(jsonAssessment)
            assessmentTemplateService.save(updatedAssessment)
            Ok(Json.toJson(updatedAssessment))
          }
          case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
        }
      }
      case JsError(_) => incorrectJsonFormat(json).toResult
    }
  }

  private def getJson(orgId: ObjectId, request: Request[AnyContent]): JsObject = (try {
    request.body.asJson.map(_.asInstanceOf[JsObject])
  } catch {
    case e: Exception => None
  }).getOrElse(Json.obj()) ++ Json.obj("orgId" -> orgId.toString)

}
