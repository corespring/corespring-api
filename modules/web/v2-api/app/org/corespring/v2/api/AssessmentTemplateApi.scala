package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.models.assessment.AssessmentTemplate
import org.corespring.models.json.JsonFormatting
import org.corespring.services.assessment.AssessmentTemplateService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ incorrectJsonFormat, cantFindAssessmentTemplateWithId }
import play.api.libs.json.{ JsError, JsSuccess, JsObject, Json }
import play.api.mvc.{ Request, AnyContent }

trait AssessmentTemplateApi extends V2Api {

  def assessmentTemplateService: AssessmentTemplateService

  def jsonFormatting: JsonFormatting

  implicit val AssessmentTemplateFormat = jsonFormatting.formatAssessmentTemplate

  def get = withIdentity { (identity, _) =>
    val t = assessmentTemplateService.find(MongoDBObject("orgId" -> identity.org.id), MongoDBObject.empty)
    Ok(Json.toJson(t))
  }

  def getById(assessmentTemplateId: ObjectId) = withIdentity { (identity, _) =>
    val query = MongoDBObject("_id" -> assessmentTemplateId, "orgId" -> identity.org.id)
    val t = assessmentTemplateService.findOne(query, MongoDBObject.empty)

    t match {
      case Some(dbResult) => Ok(Json.toJson(t))
      case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
    }
  }

  def create() = withIdentity { (identity, request) =>
    val json = getJson(identity, request)
    Json.fromJson[AssessmentTemplate](json) match {
      case JsSuccess(jsonAssessment, _) => {
        val assessmentTemplate = new AssessmentTemplate().merge(jsonAssessment)
        assessmentTemplateService.create(assessmentTemplate)
        Created(Json.prettyPrint(Json.toJson(assessmentTemplate)))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def update(assessmentTemplateId: ObjectId) = withIdentity { (identity, request) =>
    val json = getJson(identity, request)
    Json.fromJson[AssessmentTemplate](json) match {
      case JsSuccess(jsonAssessment, _) => {
        val query = MongoDBObject("_id" -> assessmentTemplateId, "orgId" -> identity.org.id)
        //TODO: RF: could $set or update work here?
        assessmentTemplateService.findOne(query, MongoDBObject.empty) match {
          case Some(dbResult) => {
            val updatedAssessment = dbResult.merge(jsonAssessment)
            assessmentTemplateService.save(updatedAssessment)
            Ok(Json.prettyPrint(Json.toJson(updatedAssessment)))
          }
          case _ => cantFindAssessmentTemplateWithId(assessmentTemplateId).toResult
        }
      }
      case JsError(_) => incorrectJsonFormat(json).toResult
    }
  }

  private def getJson(identity: OrgAndOpts, request: Request[AnyContent]): JsObject = (try {
    request.body.asJson.map(_.asInstanceOf[JsObject])
  } catch {
    case e: Exception => None
  }).getOrElse(Json.obj()) ++ Json.obj("orgId" -> identity.org.id.toString)

}
