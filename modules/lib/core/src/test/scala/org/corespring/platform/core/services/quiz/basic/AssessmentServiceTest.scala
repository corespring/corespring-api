package org.corespring.platform.core.services.assessment.basic

import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.ItemSessionSettings
import org.corespring.platform.core.models.assessment.basic.{ Answer, Participant, Question, Assessment }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.{ PlaySingleton, BaseTest }
import play.api.libs.json.Json
import org.corespring.test.utils.JsonToModel

class AssessmentServiceTest extends BaseTest with JsonToModel {

  val service = AssessmentService

  sequential

  PlaySingleton.start()

  "Assessment" should {
    "save" in {

      val count = service.count()
      val q = Assessment()
      service.create(q)
      service.count() === (count + 1)
      service.remove(q)
      service.count() === count
    }

    "parse json" in {

      val q = new Assessment(
        id = new ObjectId(),
        orgId = Some(new ObjectId()),
        metadata = Map("hello" -> "there"),
        questions = Seq(
          Question(
            itemId = VersionedId(new ObjectId()),
            settings = ItemSessionSettings())),
        participants = Seq(
          Participant(
            answers = Seq(Answer(new ObjectId(), VersionedId(new ObjectId()))),
            externalUid = "blah")))

      val json = Json.toJson(q)
      val newQ: Assessment = getData(Json.fromJson[Assessment](json))
      q.id === newQ.id
      q.orgId === newQ.orgId
      q.questions === newQ.questions
      q.participants === newQ.participants
    }

    "add answer" in {

      val q = Assessment(questions = Seq(), participants = Seq(
        Participant(externalUid = "sam.smith@gmail.com", answers = Seq())))
      service.create(q)
      val answer = Answer(new ObjectId(), VersionedId(new ObjectId()))

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

      service.addAnswer(q.id, "sam.smith@gmail.com", Answer(new ObjectId(), VersionedId(new ObjectId()))) match {
        case Some(updated) => {
          updated.participants(0).answers.length === 2
        }
        case _ => failure("couldn't find updated")
      }
    }

    "find by ids" in {

      val assessmentOne = Assessment(
        questions = Seq(),
        participants = Seq(
          Participant(
            externalUid = "sam.smith@gmail.com",
            answers = Seq())))
      service.create(assessmentOne)

      val assessmentTwo = Assessment(
        questions = Seq(),
        participants = Seq(
          Participant(
            externalUid = "sam.smith@gmail.com",
            answers = Seq())))
      service.create(assessmentTwo)
      val result = service.findByIds(List(assessmentOne.id, assessmentTwo.id))
      result.length === 2
    }

    "update adds item info" in {

      val id = VersionedId(new ObjectId("50b653a1e4b0ec03f29344b0"))
      itemService.findOneById(id) match {
        case Some(i) => {

          i.taskInfo match {
            case Some(info) => {

              val assessmentOne = Assessment(
                questions = Seq(Question(itemId = i.id)),
                participants = Seq(
                  Participant(
                    externalUid = "sam.smith@gmail.com",
                    answers = Seq())))
              service.create(assessmentOne)

              service.findOneById(assessmentOne.id) match {
                case Some(updatedAssessment) => {
                  updatedAssessment.questions(0).title === info.title
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
          case Some(assessment) => {

            val participant = assessment.participants(0)

            val completeAndScore: Seq[(Boolean, Int)] = participant.answers.map(a => {
              val json = Json.toJson(a)
              ((json \ "isComplete").as[Boolean], (json \ "score").as[Int])
            })

            completeAndScore === expected
            success
          }
          case _ => {
            failure("can't find assessment with id: 000000000000000000000001")
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

