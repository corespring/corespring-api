package basiclti.controllers

import basiclti.accessControl.auth.ValidateQuizIdAndOrgId
import basiclti.accessControl.auth.requests.OrgRequest
import basiclti.models
import basiclti.models.LtiQuiz
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.mvc._
import scala.Left
import scala.Right
import scala.Some

class LtiQuizzes(auth: ValidateQuizIdAndOrgId[OrgRequest[AnyContent]]) extends Controller {

  /** Prevent the block from executing if the cookie id doesn't match the request id */
  private def quizIdMatches(requestQuizId: ObjectId)(quizId: String, orgId: String): Boolean = {
    try {
      val oid = new ObjectId(quizId)
      oid == requestQuizId
    } catch {
      case t: Throwable => false
    }
  }

  def get(id: ObjectId) = auth.ValidatedAction(quizIdMatches(id)_) {
    request =>
      models.LtiQuiz.findOneById(id) match {
        case Some(c) => Ok(toJson(c))
        case _ => NotFound("Can't find launch config with that id")
      }
  }

  def update(id: ObjectId) = auth.ValidatedAction(quizIdMatches(id)_) {
    request =>

      quiz(request) match {
        case Some(cfg) if (id != cfg.id) => BadRequest("the json id doesn't match the url id")
        case Some(cfg) => {
          models.LtiQuiz.update(cfg, request.orgId) match {
            case Left(e) => BadRequest(e.message)
            case Right(updatedConfig) => Ok(toJson(updatedConfig))
          }
        }
        case _ => BadRequest("Invalid json provided")
      }
  }

  private def quiz(request: Request[AnyContent]): Option[LtiQuiz] = {
    request.body.asJson match {
      case Some(json) => {
        try {
          val out = json.asOpt[LtiQuiz]
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

object LtiQuizzes extends LtiQuizzes(new ValidateQuizIdAndOrgId[OrgRequest[AnyContent]] {
  override def makeRequest(orgId: ObjectId, r: Request[AnyContent]): Option[OrgRequest[AnyContent]] = Some(new OrgRequest[AnyContent](orgId, r))
})
