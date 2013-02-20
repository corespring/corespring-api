package api.v1

import org.bson.types.ObjectId
import controllers.auth.BaseApi
import securesocial.core.SecuredRequest
import play.api.mvc.{Action, Result, AnyContent}
import models.quiz.basic.Quiz

object QuizApi extends BaseApi {

  def WithQuiz(id:ObjectId, orgId : ObjectId)(f: Quiz => Result): Result = Quiz.findOneById(id) match {
    case Some(q) => {
      q.orgId match {
        case Some(quizOrgId) => if(quizOrgId == orgId) f(q) else BadRequest("You can't access this quiz")
        case _ => NotFound
      }
    }
    case _ => NotFound
  }

  def create() = ApiAction{
    request =>
      Ok("not ready")
  }

  def read(id: ObjectId) = ApiAction{
    request => WithQuiz(id, request.ctx.organization){ quiz =>
      Ok("not ready")
    }
  }

  def update(id: ObjectId) = ApiAction {
    request => Ok("not ready")
  }

  def delete(id: ObjectId) = ApiAction {
    request => Ok("not ready")
  }
}
