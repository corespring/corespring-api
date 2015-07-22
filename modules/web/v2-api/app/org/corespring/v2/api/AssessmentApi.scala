package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Answer, Assessment }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.assessment.AssessmentService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import play.api.libs.json.{ JsObject, JsSuccess, Json }
import play.api.libs.json.Json._
import play.api.mvc.{ SimpleResult, AnyContent, Request }

trait AssessmentApi extends V2Api {

  def assessmentService: AssessmentService

  def jsonFormatting: JsonFormatting

  implicit val formatAssessment = jsonFormatting.formatAssessment
  implicit val formatAnswer = jsonFormatting.formatAnswer

  def create() = withIdentity { (identity, request) =>
    val json = getAssessmentJson(identity, request)
    Json.fromJson[Assessment](json)(jsonFormatting.formatAssessment) match {
      case JsSuccess(jsonAssessment, _) => {
        val assessment = new Assessment().merge(jsonAssessment)
        assessmentService.create(assessment)
        Created(Json.prettyPrint(Json.toJson(assessment)))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  }

  def getByIds(assessmentIds: String) = withIdentity { (identity, _) =>
    val ids = assessmentIds.split(",").map(id => new ObjectId(id.trim)).toList
    val assessments = assessmentService.findByIds(ids, identity.org.id)
    ids.length match {
      case 1 => assessments.length match {
        case 1 => Ok(Json.prettyPrint(toJson(assessments.head)))
        case 0 => cantFindAssessmentWithId(ids.head).toResult
      }
      case _ => Ok(Json.prettyPrint(toJson(assessments)))
    }
  }

  def get(authorId: Option[String]) = withIdentity { (identity, _) =>
    authorId match {
      case Some(authorId) => getByAuthorId(authorId, identity.org.id)
      case _ => Ok(Json.prettyPrint(toJson(assessmentService.findAllByOrgId(identity.org.id))))
    }
  }

  def update(assessmentId: ObjectId) = withAssessment(assessmentId, { (assessment, identity, request) =>
    val json = getAssessmentJson(identity, request)
    Json.fromJson[Assessment](json) match {
      case JsSuccess(jsonAssessment, _) => {
        val newAssessment = assessment.merge(jsonAssessment)
        assessmentService.update(newAssessment)
        Ok(Json.prettyPrint(toJson(newAssessment)))
      }
      case _ => incorrectJsonFormat(json).toResult
    }
  })

  def delete(assessmentId: ObjectId) = withAssessment(assessmentId, { (assessment, _, _) =>
    assessmentService.remove(assessment)
    Ok(Json.prettyPrint(toJson(assessment)))
  })

  def addParticipants(assessmentId: ObjectId) = withAssessment(assessmentId, { (assessment, identity, request) =>
    request.body.asJson match {
      case Some(json) => {
        (json \ "ids").asOpt[Seq[String]] match {
          case Some(ids) => {
            val updated = assessmentService.addParticipants(assessmentId, ids)
            Ok(Json.prettyPrint(toJson(updated)))
          }
          case _ => incorrectJsonFormat(json).toResult
        }
      }
      case _ => noJson.toResult
    }
  })

  def addAnswer(assessmentId: ObjectId, externalId: Option[String]) =
    withAssessment(assessmentId, { (assessment, identity, request) =>
      externalId match {
        case Some(id) => {
          (try {
            request.body.asJson.map(_.asInstanceOf[JsObject])
          } catch {
            case e: Exception => None
          }) match {
            case Some(json) => Json.fromJson[Answer](json) match {
              case JsSuccess(answer, _) => {
                assessmentService.addAnswer(assessmentId, id, answer) match {
                  case Some(updatedAssessment) => Ok(Json.prettyPrint(Json.toJson(updatedAssessment)))
                  case _ => incorrectJsonFormat(json).toResult
                }
              }
              case _ => incorrectJsonFormat(json).toResult
            }
            case None => noJson.toResult
          }
        }
        case _ => addAnswerRequiresId(assessmentId).toResult
      }
    })

  private def getByAuthorId(authorId: String, organizationId: ObjectId) =
    Ok(Json.prettyPrint(toJson(assessmentService.findByAuthorAndOrg(authorId, organizationId))))

  private def getAssessmentJson(identity: OrgAndOpts, request: Request[AnyContent]): JsObject = (try {
    request.body.asJson.map(_.asInstanceOf[JsObject])
  } catch {
    case e: Exception => None
  }).getOrElse(Json.obj()) ++ Json.obj("orgId" -> identity.org.id.toString)

  private def withAssessment(assessmentId: ObjectId, block: ((Assessment, OrgAndOpts, Request[AnyContent]) => SimpleResult)) =
    withIdentity { (identity, request) =>
      assessmentService.findByIdAndOrg(assessmentId, identity.org.id) match {
        case Some(assessment) => block(assessment, identity, request)
        case _ => cantFindAssessmentWithId(assessmentId).toResult
      }
    }

}
