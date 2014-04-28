package basiclti.controllers

import basiclti.accessControl.auth.ValidateAssessmentIdAndOrgId
import basiclti.accessControl.auth.requests.OrgRequest
import basiclti.models
import basiclti.models.LtiAssessment
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.mvc._
import scala.Left
import scala.Right
import scala.Some
import scala.concurrent.{ExecutionContext, Future}

class LtiAssessments(auth: ValidateAssessmentIdAndOrgId[OrgRequest[AnyContent]]) extends Controller {

  import ExecutionContext.Implicits.global

  /** Prevent the block from executing if the cookie id doesn't match the request id */
  private def assessmentIdMatches(requestAssessmentId: ObjectId)(assessmentId: String, orgId: String): Boolean = {
    try {
      val oid = new ObjectId(assessmentId)
      oid == requestAssessmentId
    } catch {
      case t: Throwable => false
    }
  }

  def get(id: ObjectId) = auth.ValidatedAction(assessmentIdMatches(id)_) {
    request =>
      Future( models.LtiAssessment.findOneById(id) match {
        case Some(c) => Ok(toJson(c))
        case _ => NotFound("Can't find launch config with that id")
      })
  }

  def update(id: ObjectId) = auth.ValidatedAction(assessmentIdMatches(id)_) {
    request =>

      Future(assessment(request) match {
        case Some(cfg) if (id != cfg.id) => BadRequest("the json id doesn't match the url id")
        case Some(cfg) => {
          models.LtiAssessment.update(cfg, request.orgId) match {
            case Left(e) => BadRequest(e.message)
            case Right(updatedConfig) => Ok(toJson(updatedConfig))
          }
        }
        case _ => BadRequest("Invalid json provided")
      })
  }

  private def assessment(request: Request[AnyContent]): Option[LtiAssessment] = {
    request.body.asJson match {
      case Some(json) => {
        try {
          val out = json.asOpt[LtiAssessment]
          out
        } catch {
          case e: Throwable => {
            play.Logger.warn(e.getMessage)
            None
          }
        }
      }
      case _ => None
    }
  }
}

object LtiAssessments extends LtiAssessments(new ValidateAssessmentIdAndOrgId[OrgRequest[AnyContent]] {
  override def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[OrgRequest[AnyContent]] = Some(new OrgRequest[AnyContent](orgId, r))
})
