package tests.models.quiz.basic

import org.specs2.mutable.{After, Specification}
import tests.{BaseTest, PlaySingleton}
import play.api.libs.json.{JsArray, Json}
import models.quiz.basic.{Answer, Participant, Question, Quiz}
import org.bson.types.ObjectId
import models.itemSession.ItemSessionSettings
import com.mongodb.casbah.commons.MongoDBObject
import common.seed.SeedDb

class QuizTest extends BaseTest{

  PlaySingleton.start()

  "Quiz" should {
    "save" in {

      val count = Quiz.count()
      val q = Quiz()
      Quiz.create(q)
      Quiz.count() === (count + 1)
      Quiz.remove(q)
      Quiz.count() === count
    }


    "parse json" in {

      val q = new Quiz(
        id = new ObjectId(),
        orgId = Some(new ObjectId()),
        metadata = Map("hello" -> "there"),
        questions = Seq(
          Question(
            itemId = new ObjectId(),
            settings = ItemSessionSettings()
          )
        ),
        participants = Seq(
          Participant(
            answers = Seq(Answer(new ObjectId(), new ObjectId())),
            externalUid = "blah"
          )
        )
      )

      val json = Json.toJson(q)
      val newQ = Json.fromJson[Quiz](json)
      q.id === newQ.id
      q.orgId === newQ.orgId
      q.questions === newQ.questions
      q.participants === newQ.participants
    }


    "add answer" in {

      Quiz.removeAll()
      val q = Quiz(questions = Seq(), participants = Seq(
        Participant(externalUid = "sam.smith@gmail.com", answers = Seq())
      ))
      Quiz.create(q)
      val answer = Answer(new ObjectId(), new ObjectId())

      Quiz.addAnswer(q.id, "sam.smith@gmail.com", answer) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 1
        }
        case _ => failure("couldn't find updated")
      }

      Quiz.addAnswer(q.id, "sam.smith@gmail.com", answer) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 1
        }
        case _ => failure("couldn't find updated")
      }

      Quiz.addAnswer(q.id, "sam.smith@gmail.com", Answer(new ObjectId(), new ObjectId())) match {
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
            answers = Seq()
          )
        ))
      Quiz.create(quizOne)

      val quizTwo = Quiz(
        questions = Seq(),
        participants = Seq(
          Participant(
            externalUid = "sam.smith@gmail.com",
            answers = Seq()
          )
        ))
      Quiz.create(quizTwo)
      val result = Quiz.findByIds(List(quizOne.id, quizTwo.id))
      result.length === 2
    }

    "update adds item info" in {

      val queryItem = MongoDBObject("_id" -> new ObjectId("50b653a1e4b0ec03f29344b0"))
      itemService.findOne(queryItem) match {
        case Some(i) => {

          i.taskInfo match {
            case Some(info) => {

              val quizOne = Quiz(
                questions = Seq(Question(itemId = i.id)),
                participants = Seq(
                  Participant(
                    externalUid = "sam.smith@gmail.com",
                    answers = Seq()
                  )
                ))
              Quiz.create(quizOne)

              Quiz.findOneById(quizOne.id) match {
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

      SeedDb.emptyData()
      SeedDb.seedData("conf/seed-data/test")

      def assertCompleteAndScore(id: ObjectId, expected: (Boolean, Int)*): org.specs2.execute.Result = {

        Quiz.findOneById(id) match {
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
      assertCompleteAndScore(new ObjectId("000000000000000000000002"), (true, 50))
      assertCompleteAndScore(new ObjectId("000000000000000000000003"), (true, 100))

    }
  }
}

