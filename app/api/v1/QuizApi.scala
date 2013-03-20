package api.v1

import org.bson.types.ObjectId
import controllers.auth.BaseApi
import play.api.mvc.{Action, Result}
import models.quiz.basic.{Participant, Answer, Quiz}
import play.api.libs.json.Json._
import api.ApiError
import models.itemSession.ItemSession

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

  def list() = ApiAction {
    request =>
      val quizzes = Quiz.findAllByOrgId(request.ctx.organization)
      Ok(toJson(quizzes))
  }

  //todo: make this apiaction
  def getResults(id: ObjectId) = Action {
    request =>
      Quiz.findOneById(id) match {
        case Some(q) => {
          def getParticipantResults(p:Participant) = {
            val scores = p.answers.map { a =>
              val score = ItemSession.get(a.sessionId) match {
                case Some(session) => ItemSession.getTotalScore(session)
                case None => (0, 0)
              }
              toJson(Map("itemId"->toJson(a.itemId.toString), "sessionId"->toJson(a.sessionId.toString), "score"->toJson(score._1.toString)))
            }
            toJson(Map("email"->toJson(p.externalUid), "name"->toJson(p.metadata("studentName")), "scores"->toJson(scores)))
          }
          Ok(toJson(q.participants.map(getParticipantResults)))
        }
        case _ => NotFound("Can't find quiz")
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
