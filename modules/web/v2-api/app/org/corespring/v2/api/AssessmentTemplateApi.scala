package org.corespring.v2.api

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.core.services.assessment.template.AssessmentTemplateService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ incorrectJsonFormat, cantFindAssessmentTemplateWithId }
import play.api.libs.json.{ JsSuccess, JsObject, Json }
import play.api.mvc.{ Request, AnyContent }

trait AssessmentTemplateApi extends V2Api {

  def assessmentTemplateService: AssessmentTemplateService

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  def get = withIdentity { (identity, _) =>
    Ok(Json.prettyPrint(Json.toJson(
      assessmentTemplateService.find(MongoDBObject("orgId" -> identity.org.id)).map(_.toAssessmentTemplate).toSeq)))
  }

  def getById(assessmentTemplateId: ObjectId) = withIdentity { (identity, _) =>
    assessmentTemplateService.findOne(MongoDBObject("_id" -> assessmentTemplateId, "orgId" -> identity.org.id)) match {
      case Some(dbResult) => Ok(Json.prettyPrint(Json.toJson(dbResult.toAssessmentTemplate)))
      case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
    }
  }

  def create() = withIdentity { (identity, request) =>
    val json = getJson(identity, request)
    Json.fromJson[AssessmentTemplate](json) match {
      case JsSuccess(jsonAssessment, _) => {
        val assessmentTemplate = new AssessmentTemplate().merge(jsonAssessment)
        assessmentTemplateService.create(assessmentTemplate.forSalat)
        Created(Json.prettyPrint(Json.toJson(assessmentTemplate)))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def update(assessmentTemplateId: ObjectId) = withIdentity { (identity, request) =>
    Json.fromJson[AssessmentTemplate](getJson(identity, request)) match {
      case JsSuccess(jsonAssessment, _) =>
        assessmentTemplateService.findOne(MongoDBObject("_id" -> assessmentTemplateId, "orgId" -> identity.org.id)) match {
          case Some(dbResult) => {
            val updatedAssessment = dbResult.toAssessmentTemplate.merge(jsonAssessment)
            assessmentTemplateService.save(updatedAssessment.forSalat)
            Ok(Json.prettyPrint(Json.toJson(updatedAssessment)))
          }
          case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
        }
    }
  }

  private def getJson(identity: OrgAndOpts, request: Request[AnyContent]): JsObject = (try {
    request.body.asJson.map(_.asInstanceOf[JsObject])
  } catch {
    case e: Exception => None
  }).getOrElse(Json.obj()) ++ Json.obj("orgId" -> identity.org.id.toString)

}
