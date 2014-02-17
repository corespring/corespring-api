package tests.api.v1

import org.bson.types.ObjectId
import org.corespring.platform.core.models.assessment.basic.{Participant, Assessment}
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.test.PlaySingleton
import org.specs2.mutable.Specification
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.{AnyContentAsJson, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Call
import org.corespring.test.utils.RequestCalling
import org.joda.time.DateTime

class AssessmentApiTest extends Specification with RequestCalling {

  PlaySingleton.start()

  val orgId = new ObjectId("51114b307fc1eaa866444648")
  val authorId = "fd707fc3c"

  val Api = api.v1.AssessmentApi

  "AssessmentApi" should {
    "get" in {
      val q = createAssessment()
      val requestedAssessment = invokeCall[Assessment](Api.get(q.id), AnyContentAsEmpty)
      requestedAssessment.id === q.id
    }

    "get multiple" in {
      val assessmentOne = createAssessment()
      val assessmentTwo = createAssessment()
      val ids = List(assessmentOne, assessmentTwo).map(_.id.toString).mkString(",")
      val multiple = invokeCall[List[Assessment]](Api.getMultiple(ids), AnyContentAsEmpty)
      multiple.length === 2
    }

    "get multiple and filters if the org isn't applicable" in {
      val assessmentOne = createAssessment()
      val assessmentTwo = createAssessment()
      val assessmentThree = Assessment(orgId = Some(new ObjectId("51114b307fc1eaa866444649")))
      AssessmentService.create(assessmentThree)
      val ids = List(assessmentOne, assessmentTwo, assessmentThree).map(_.id.toString).mkString(",")
      val multiple = invokeCall[List[Assessment]](Api.getMultiple(ids), AnyContentAsEmpty)
      multiple.length === 2
    }

    "get by author" in {
      AssessmentService.findByAuthor(authorId).length === 1
    }

    "create" in {
      val q = createAssessment(Map("course" -> "some course"))
      val assessment = q.copy(participants = Seq(
        Participant(
          externalUid = "ed",
          answers = Seq()
        )
      ))
      val json = AnyContentAsJson(Json.toJson(assessment))
      val createdAssessment = invokeCall[Assessment](Api.create(), json)
      createdAssessment.metadata.get("course") === Some("some course")
      createdAssessment.participants.length === 1
      createdAssessment.starts.isDefined must beTrue
      createdAssessment.ends.isDefined must beTrue
    }

    "update" in {
      val q = createAssessment(Map("course" -> "some course"))
      val update = q.copy(metadata = Map("course" -> "some updated course"))
      val json = AnyContentAsJson(Json.toJson(update))
      val updatedAssessment = invokeCall[Assessment](Api.update(q.id), json)
      updatedAssessment.metadata.get("course") === Some("some updated course")
    }

    def createAssessment(metadata: Map[String, String] = Map()): Assessment = {
      val q = Assessment(orgId = Some(orgId), metadata = metadata, starts = Some(new DateTime()), ends = Some(new DateTime()))
      AssessmentService.create(q)
      q
    }

    "delete" in {
      val q = createAssessment(Map("course" -> "some course"))

      val call: Call = api.v1.routes.AssessmentApi.delete(q.id)
      route(FakeRequest(call.method, call.url, FakeAuthHeader, AnyContentAsEmpty)) match {
        case Some(result) => {
          status(result) === OK
          AssessmentService.findOneById(q.id) === None
        }
        case _ => failure("Error deleting")
      }
    }

    "add participant" in {
      val q = createAssessment()
      val json = AnyContentAsJson(Json.toJson(Map("ids" -> Json.toJson(Seq("50c9f79db519c8996618447d", "50be107ae4b954fe2326ab72", "50ba1c504eda5d94372233c7")))))
      val updatedAssessment = invokeCall[Assessment](Api.addParticipants(q.id), json)
      updatedAssessment.participants.length shouldEqual 3
    }

    "list" in {

      AssessmentService.removeAll()
      createAssessment()
      createAssessment()
      createAssessment()

      val call: Call = api.v1.routes.AssessmentApi.list()
      route(FakeRequest(call.method, call.url, FakeAuthHeader, AnyContentAsEmpty)) match {
        case Some(result) => {
          status(result) === OK
          val json = Json.parse(contentAsString(result))
          Json.fromJson[List[Assessment]](json) match {
            case JsSuccess(assessments, _) => {
              assessments.length === 3
            }
            case _ => failure("Couldn't parse json")
          }
        }
        case _ => failure("Error deleting")
      }
    }
  }
}
