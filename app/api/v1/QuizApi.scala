package api.v1

import org.bson.types.ObjectId
import controllers.auth.BaseApi
import play.api.mvc.{Action, Result}
import models.quiz.basic.{Participant, Answer, Quiz}
import play.api.libs.json.Json._
import api.ApiError
import models.itemSession.ItemSession
import play.api.libs.json.{JsUndefined, JsValue}

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

  def getMultiple(ids:String) = ApiAction{
    request => {
      val objectIds = ids.split(",").toList.map(new ObjectId(_))
      val quizzes : List[Quiz] = Quiz.findByIds(objectIds)
      val filtered = quizzes.filter(_.orgId == Some(request.ctx.organization))
      Ok(toJson(filtered))
    }
  }

  def update(id: ObjectId) = ApiAction {
    request => WithQuiz(id, request.ctx.organization) {
      quiz =>
        request.body.asJson match {
          case Some(json) => {
            val jsonQuiz = fromJson[Quiz](json)
            val newQuiz = quiz.copy(
              participants = (json \ "participants") match {
                case p:JsUndefined => quiz.participants
                case _ => jsonQuiz.participants
              },
              questions = (json \ "questions") match {
                case p:JsUndefined => quiz.questions
                case _ => jsonQuiz.questions
              },
              metadata = (json \ "metadata") match {
                case p:JsUndefined => quiz.metadata
                case _ => jsonQuiz.metadata
              }

            )
            Quiz.update(newQuiz)
            Ok(toJson(newQuiz))
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

  def list() = ApiAction {
    request =>
      val quizzes = Quiz.findAllByOrgId(request.ctx.organization)
      Ok(toJson(quizzes))
  }

  def addParticipants(id:ObjectId) = ApiAction {
    request => WithQuiz(id,request.ctx.organization){
      quiz =>
        request.body.asJson match {
          case Some(json)  =>
            val ids = (json \ "ids").as[Seq[String]]
            val updated = Quiz.addParticipants(quiz, ids)
            Ok(toJson(updated))
          case _ =>
            BadRequest(toJson(ApiError.JsonExpected))
        }
    }
  }

  def addAnswerForParticipant(quizId: ObjectId, externalUid: String) = ApiAction {
    request => {
      WithQuiz(quizId, request.ctx.organization) {
        quiz =>
          request.body.asJson match {
            case Some(json) => {
              val answer = fromJson[Answer](json)
              val updated = Quiz.addAnswer(quizId, externalUid, answer)
              Ok(toJson(updated))
            }
            case _ => BadRequest(toJson(ApiError.JsonExpected))
          }
      }
    }
  }
}
