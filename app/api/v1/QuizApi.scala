package api.v1

import api.ApiError
import controllers.auth.BaseApi
import org.bson.types.ObjectId
import org.corespring.platform.core.models.quiz.basic.{ Answer, Quiz }
import org.corespring.platform.core.services.quiz.basic.QuizService
import play.api.libs.json.Json._
import play.api.mvc.Result

class QuizApi(quizService: QuizService) extends BaseApi {

  def WithQuiz(id: ObjectId, orgId: ObjectId)(f: Quiz => Result): Result = quizService.findOneById(id) match {
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
      parsed[Quiz](request.body.asJson, {
        quiz =>
          val copy = quiz.copy(orgId = Some(request.ctx.organization))
          println(copy.metadata)
          quizService.create(copy)
          Ok(toJson(copy))
      })
  }

  def get(id: ObjectId) = ApiAction {
    request =>
      WithQuiz(id, request.ctx.organization) {
        quiz =>
          Ok(toJson(quiz))
      }
  }

  def getMultiple(ids: String) = ApiAction {
    request =>
      {
        val objectIds = ids.split(",").toList.map(new ObjectId(_))
        val quizzes: List[Quiz] = quizService.findByIds(objectIds)
        val filtered = quizzes.filter(_.orgId == Some(request.ctx.organization))
        Ok(toJson(filtered))
      }
  }

  def update(id: ObjectId) = ApiAction {
    request =>
      WithQuiz(id, request.ctx.organization) {
        quiz =>
          parsed[Quiz](request.body.asJson, {
            jsonQuiz =>
              val newQuiz = quiz.copy(
                participants = if (jsonQuiz.participants.length > 0) jsonQuiz.participants else quiz.participants,
                questions = if (jsonQuiz.questions.length > 0) jsonQuiz.questions else quiz.questions,
                metadata = if (jsonQuiz.metadata.size > 0) jsonQuiz.metadata else quiz.metadata)
              quizService.update(newQuiz)
              Ok(toJson(newQuiz))
          })
      }
  }

  def delete(id: ObjectId) = ApiAction {
    request =>
      {
        WithQuiz(id, request.ctx.organization) {
          quiz =>
            quizService.remove(quiz)
            Ok("")
        }
      }
  }

  def list() = ApiAction {
    request =>
      val quizzes = quizService.findAllByOrgId(request.ctx.organization)
      Ok(toJson(quizzes))
  }

  def addParticipants(id: ObjectId) = ApiAction {
    request =>
      request.body.asJson match {
        case Some(json) =>
          val ids = (json \ "ids").as[Seq[String]]
          val updated = quizService.addParticipants(id, ids)
          Ok(toJson(updated))

        case _ =>
          BadRequest(toJson(ApiError.JsonExpected))
      }
  }

  def addAnswerForParticipant(quizId: ObjectId, externalUid: String) = ApiAction {
    request =>
      {
        WithQuiz(quizId, request.ctx.organization) {
          quiz =>
            parsed[Answer](request.body.asJson, {
              answer =>
                val updated = quizService.addAnswer(quizId, externalUid, answer)
                Ok(toJson(updated))
            })
        }
      }
  }
}

object QuizApi extends QuizApi(QuizService)
