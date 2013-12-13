package org.corespring.platform.core.services.quiz.basic

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.platform.core.models.quiz.basic.{ Answer, Participant, Question, Quiz }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.{ PlaySingleton, BaseTest }
import play.api.libs.json.Json
import org.corespring.test.utils.JsonToModel

class QuizServiceTest extends BaseTest with JsonToModel {

  val service = QuizService

  sequential

  PlaySingleton.start()

  "Quiz" should {
    "save" in {

      val count = service.count()
      val q = Quiz()
      service.create(q)
      service.count() === (count + 1)
      service.remove(q)
      service.count() === count
    }

    "parse json" in {

      val q = new Quiz(
        id = new ObjectId(),
        orgId = Some(new ObjectId()),
        metadata = Map("hello" -> "there"),
        questions = Seq(
          Question(
            itemId = VersionedId(new ObjectId()),
            settings = ItemSessionSettings())),
        participants = Seq(
          Participant(
            answers = Seq(Answer(new ObjectId(), new ObjectId())),
            externalUid = "blah")))

      val json = Json.toJson(q)
      val newQ: Quiz = getData(Json.fromJson[Quiz](json))
      q.id === newQ.id
      q.orgId === newQ.orgId
      q.questions === newQ.questions
      q.participants === newQ.participants
    }

    "add answer" in {

      val q = Quiz(questions = Seq(), participants = Seq(
        Participant(externalUid = "sam.smith@gmail.com", answers = Seq())))
      service.create(q)
      val answer = Answer(new ObjectId(), new ObjectId())

      service.addAnswer(q.id, "sam.smith@gmail.com", answer) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 1
        }
        case _ => failure("couldn't find updated")
      }

      service.addAnswer(q.id, "sam.smith@gmail.com", answer) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 1
        }
        case _ => failure("couldn't find updated")
      }

      service.addAnswer(q.id, "sam.smith@gmail.com", Answer(new ObjectId(), new ObjectId())) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 2
        }
        case _ => failure("couldn't find updated")
      }
    }

    "find by ids" in {

      val quizOne = Quiz(
        questions = Seq(),
        participants = Seq(
          Participant(
            externalUid = "sam.smith@gmail.com",
            answers = Seq())))
      service.create(quizOne)

      val quizTwo = Quiz(
        questions = Seq(),
        participants = Seq(
          Participant(
            externalUid = "sam.smith@gmail.com",
            answers = Seq())))
      service.create(quizTwo)
      val result = service.findByIds(List(quizOne.id, quizTwo.id))
      result.length === 2
    }

    "update adds item info" in {

      val id = VersionedId(new ObjectId("50b653a1e4b0ec03f29344b0"))
      itemService.findOneById(id) match {
        case Some(i) => {

          i.taskInfo match {
            case Some(info) => {

              val quizOne = Quiz(
                questions = Seq(Question(itemId = i.id)),
                participants = Seq(
                  Participant(
                    externalUid = "sam.smith@gmail.com",
                    answers = Seq())))
              service.create(quizOne)

              service.findOneById(quizOne.id) match {
                case Some(updatedQuiz) => {
                  updatedQuiz.questions(0).title === info.title
                  success
                }
                case _ => failure
              }
            }
            case _ => failure
          }

        }
        case _ => failure("couldn't find an item")
      }
    }

    "json generation works" in {

      def assertCompleteAndScore(id: ObjectId, expected: (Boolean, Int)*): org.specs2.execute.Result = {

        service.findOneById(id) match {
          case Some(quiz) => {

            val participant = quiz.participants(0)

            val completeAndScore: Seq[(Boolean, Int)] = participant.answers.map(a => {
              val json = Json.toJson(a)
              ((json \ "isComplete").as[Boolean], (json \ "score").as[Int])
            })

            completeAndScore === expected
            success
          }
          case _ => {
            failure("can't find quiz with id: 000000000000000000000001")
            failure
          }
        }
      }

      assertCompleteAndScore(new ObjectId("000000000000000000000001"), (false, 0))
      assertCompleteAndScore(new ObjectId("000000000000000000000002"), (true, 25))
      assertCompleteAndScore(new ObjectId("000000000000000000000003"), (true, 50))

    }

  }
}

