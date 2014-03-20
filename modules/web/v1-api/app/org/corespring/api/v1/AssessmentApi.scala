package org.corespring.api.v1

import org.bson.types.ObjectId
import org.corespring.api.v1.errors.ApiError
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.assessment.basic.{ Answer, Assessment }
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import play.api.libs.json.Json._
import play.api.mvc.Result

class AssessmentApi(assessmentService: AssessmentService) extends BaseApi {

  def WithAssessment(id: ObjectId, orgId: ObjectId)(f: Assessment => Result): Result = assessmentService.findOneById(id) match {
    case Some(q) => {
      q.orgId match {
        case Some(assessmentOrgId) => if (assessmentOrgId == orgId) f(q) else BadRequest("You can't access this assessment")
        case _ => NotFound
      }
    }
    case _ => NotFound
  }

  def create() = ApiAction {
    request =>
      parsed[Assessment](request.body.asJson, {
        assessment =>
          val copy = assessment.copy(orgId = Some(request.ctx.organization))
          assessmentService.create(copy)
          Ok(toJson(copy))
      })
  }

  def get(id: ObjectId) = ApiAction {
    request =>
      WithAssessment(id, request.ctx.organization) {
        assessment =>
          Ok(toJson(assessment))
      }
  }

  def getByAuthor(authorId: String) = ApiAction { request =>
    Ok(toJson(assessmentService.findByAuthor(authorId)))
  }

  def getMultiple(ids: String) = ApiAction {
    request =>
      {
        val objectIds = ids.split(",").toList.map(new ObjectId(_))
        val assessments: List[Assessment] = assessmentService.findByIds(objectIds)
        val filtered = assessments.filter(_.orgId == Some(request.ctx.organization))
        Ok(toJson(filtered))
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>
      WithAssessment(id, request.ctx.organization) {
        assessment =>
          parsed[Assessment](request.body.asJson, {
            jsonAssessment =>
              val newAssessment = assessment.copy(
                participants = if (jsonAssessment.participants.length > 0) jsonAssessment.participants else assessment.participants,
                questions = if (jsonAssessment.questions.length > 0) jsonAssessment.questions else assessment.questions,
                metadata = if (jsonAssessment.metadata.size > 0) jsonAssessment.metadata else assessment.metadata)
              assessmentService.update(newAssessment)
              Ok(toJson(newAssessment))
          })
      }
  }

  def delete(id: ObjectId) = ApiAction {
    request =>
      {
        WithAssessment(id, request.ctx.organization) {
          assessment =>
            assessmentService.remove(assessment)
            Ok("")
        }
      }
  }

  def list() = ApiAction {
    request =>
      val assessments = assessmentService.findAllByOrgId(request.ctx.organization)
      Ok(toJson(assessments))
  }

  def addParticipants(id: ObjectId) = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) =>
          val ids = (json \ "ids").as[Seq[String]]
          val updated = assessmentService.addParticipants(id, ids)
          Ok(toJson(updated))

        case _ =>
          BadRequest(toJson(ApiError.JsonExpected))
      }
  }

  def addAnswerForParticipant(assessmentId: ObjectId, externalUid: String) = ApiAction {
    request =>
      {
        WithAssessment(assessmentId, request.ctx.organization) {
          assessment =>
            parsed[Answer](request.body.asJson, {
              answer =>
                val updated = assessmentService.addAnswer(assessmentId, externalUid, answer)
                Ok(toJson(updated))
            })
        }
      }
  }
}

object AssessmentApi extends AssessmentApi(AssessmentService)
