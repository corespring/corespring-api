package api.v1

import org.bson.types.ObjectId
import controllers.auth.BaseApi
import play.api.mvc.Result
import models.quiz.basic.Quiz
import play.api.libs.json.Json._

object QuizApi extends BaseApi {

  def WithQuiz(id: ObjectId, orgId: ObjectId)(f: Quiz => Result): Result = Quiz.findOneById(id) match {
    case Some(q) => {
      q.orgId match {
        case Some(quizOrgId) => if (quizOrgId == orgId) f(q) else BadRequest("You can't access this quiz")
        case _ => NotFound
      }
    }
    case _ => NotFound
  }

  def create() = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) => {
          val quiz = fromJson[Quiz](json)
          val copy = quiz.copy(orgId = Some(request.ctx.organization))
          Quiz.create(copy)
          Ok(toJson(copy))
        }
        case _ => BadRequest("Invalid Json")
      }
  }

  def get(id: ObjectId) = ApiAction {
    request => WithQuiz(id, request.ctx.organization) {
      quiz =>
        Ok(toJson(quiz))
    }
  }

  def update(id: ObjectId) = ApiAction {
    request => WithQuiz(id, request.ctx.organization) {
      quiz =>
        request.body.asJson match {
          case Some(json) => {
            val quiz = fromJson[Quiz](json)
            Quiz.update(quiz)
            Ok(toJson(quiz))
          }
          case _ => BadRequest("invalid json")
        }
    }
  }

  def delete(id: ObjectId) = ApiAction {
    request => {
      WithQuiz(id, request.ctx.organization) {
        quiz =>
          Quiz.remove(quiz)
          Ok("")
      }
    }
  }

  def list() = ApiAction{
    request =>
      val quizzes = Quiz.findAllByOrgId(request.ctx.organization)
      Ok(toJson(quizzes))
  }
}
