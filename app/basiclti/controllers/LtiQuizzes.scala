package basiclti.controllers

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import basiclti.models.LtiQuiz

import play.api.libs.json.Json._
import play.api.mvc.AnyContent
import play.api.mvc.Request
import basiclti.models

object LtiQuizzes extends BaseApi {

  def get(id: ObjectId) = ApiAction {
    request =>

      models.LtiQuiz.findOneById(id) match {
        case Some(c) => Ok(toJson(c))
        case _ => NotFound("Can't find launch config with that id")
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>

      quiz(request) match {
        case Some(cfg) if (id != cfg.id) => BadRequest("the json id doesn't match the url id")
        case Some(cfg) => {
          models.LtiQuiz.update(cfg, request.ctx.organization) match {
            case Left(e) => BadRequest("Error updating")
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
        }
        catch {
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
