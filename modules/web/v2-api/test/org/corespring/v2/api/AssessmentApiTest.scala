package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item.FieldValue
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.assessment.{ Answer, Participant, Assessment }
import org.corespring.models.json.JsonFormatting
import org.corespring.services.assessment.AssessmentService
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.models.{ MockFactory, OrgAndOpts }
import org.corespring.v2.errors.Errors.invalidToken
import org.corespring.v2.errors.V2Error
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsObject }
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration.Duration
import scalaz.{ Failure, Success, Validation }

class AssessmentApiTest extends Specification with MockFactory {

  PlaySingleton.start()

  val jsonFormatting = new JsonFormatting {
    override def fieldValue: FieldValue = ???

    override def findStandardByDotNotation: (String) => Option[Standard] = ???

    override def countItemsInCollection(collectionId: ObjectId): Long = ???

    override def rootOrgId: ObjectId = ???

    override def findSubjectById: (ObjectId) => Option[Subject] = ???
  }

  import jsonFormatting._

  case class apiScope(orgAndOpts: Option[OrgAndOpts] = Some(mockOrgAndOpts()),
    id: Option[ObjectId] = None,
    ids: List[ObjectId] = List.empty[ObjectId],
    authorId: Option[String] = None,
    participants: Seq[String] = Seq.empty[String]) extends Scope {
    val orgId = orgAndOpts.map(_.org.id)
    def assessmentFor(id: ObjectId) =
      new Assessment(id = id, orgId = orgId, participants = participants.map(id => Participant(Seq(), id)))
    val assessments = ids.map(id => assessmentFor(id))
    val allIds = (id match {
      case Some(id) => ids :+ id
      case _ => ids
    })

    val assessmentService = {
      val m = mock[AssessmentService]
      orgId match {
        case Some(orgId) => {
          m.findByIds(any[List[ObjectId]], any[ObjectId]) returns assessments
          allIds.foreach(id => {
            m.findByIdAndOrg(id, orgId) returns Some(assessmentFor(id))
          })
          m.findAllByOrgId(orgId) returns assessments
          authorId match {
            case Some(authorId) => {
              m.findByAuthorAndOrg(authorId, orgId) returns assessments.map(_.copy(metadata = Map("author" -> authorId)))
            }
            case _ => {}
          }
        }
        case _ => {}
      }
      m.addParticipants(any[ObjectId], any[Seq[String]]) answers { (args, _) =>
        {
          val argArray = args.asInstanceOf[Array[Object]]
          val id = argArray(0).asInstanceOf[ObjectId]
          val ids = argArray(1).asInstanceOf[Seq[String]]
          val assessment = assessmentFor(id)
          Some(assessment.copy(participants = assessment.participants ++ ids.map(id => Participant(Seq(), id))))
        }
      }
      m.addAnswer(any[ObjectId], any[String], any[Answer]) answers { (args, _) =>
        {
          val argArray = args.asInstanceOf[Array[Object]]
          val answer = argArray(2).asInstanceOf[Answer]

          def processParticipants(externalUid: String)(p: Participant): Participant = {
            if (p.externalUid == externalUid && !p.answers.exists(_.itemId == answer.itemId)) {
              p.copy(answers = p.answers :+ answer)
            } else {
              p
            }
          }

          val assessmentId = argArray(0).asInstanceOf[ObjectId]
          val externalUid = argArray(1).asInstanceOf[String]
          val assessment = assessmentFor(assessmentId)
          Some(assessment.copy(participants = assessment.participants.map(processParticipants(externalUid))))
        }
      }
      m
    }
    val assessmentApi = new AssessmentApi {
      override def assessmentService: AssessmentService = apiScope.this.assessmentService
      override implicit def ec: ExecutionContext = ExecutionContext.global
      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = {
        orgAndOpts match {
          case Some(orgAndOpts) => Success(orgAndOpts)
          case _ => Failure(invalidToken(FakeRequest()))
        }
      }
    }
  }

  "create" should {

    "without identity" should {

      "return 401" in new apiScope(orgAndOpts = None) {
        status(assessmentApi.create()(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }

    }

    "with identity" should {

      "call assessmentService#create" in new apiScope() {
        Await.result(assessmentApi.create()(FakeRequest()), Duration.Inf)
        there was one(assessmentService).create(any[Assessment])
      }

      "return 201" in new apiScope() {
        status(assessmentApi.create()(FakeRequest())) must be equalTo (CREATED)
      }

      "return created Assessment as JSON" in new apiScope() {
        val json = contentAsJson(assessmentApi.create()(FakeRequest()))
        (json \ "id").asOpt[String] must not beEmpty;
        (json \ "orgId").asOpt[String] must be equalTo (orgAndOpts.map(_.org.id.toString))
      }

    }

  }

  "getByIds" should {

    val ids = List(new ObjectId(), new ObjectId())

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None) {
        status(assessmentApi.getByIds(ids.map(_.toString).mkString(","))(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "return 200" in new apiScope(ids = ids) {
        status(assessmentApi.getByIds(ids.map(_.toString).mkString(","))(FakeRequest())) must be equalTo (OK)
      }

      "return JSON for Assessments with each id" in new apiScope(ids = ids) {
        val json = contentAsJson(assessmentApi.getByIds(ids.map(_.toString).mkString(","))(FakeRequest()))
        json.as[Seq[JsObject]].map(o => (o \ "id").as[String]) must be equalTo (ids.map(_.toString))
      }

      "with a single id" should {

        "return 200" in new apiScope(ids = ids) {
          status(assessmentApi.getByIds(ids.map(_.toString).mkString(","))(FakeRequest())) must be equalTo (OK)
        }

        "return single json object for id" in new apiScope(ids = List(ids.head)) {
          val json = contentAsJson(assessmentApi.getByIds(ids.map(_.toString).mkString(","))(FakeRequest()))
          (json.as[JsObject] \ "id").as[String] must be equalTo (ids.head.toString)
        }

      }

    }

  }

  "get" should {

    val ids = List(new ObjectId(), new ObjectId())
    val authorId = Some("abc123")

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None) {
        status(assessmentApi.get(None)(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "return 200" in new apiScope(ids = ids) {
        status(assessmentApi.get(None)(FakeRequest())) must be equalTo (OK)
      }

      "return result of assessmentService#getAllByOrgId" in new apiScope(ids = ids) {
        val json = contentAsJson(assessmentApi.get(None)(FakeRequest()))
        there was one(assessmentService).findAllByOrgId(orgId.get)
        json must be equalTo (Json.toJson(assessments))
      }

      "with authorId" should {

        "return 200" in new apiScope(ids = ids, authorId = authorId) {
          status(assessmentApi.get(authorId)(FakeRequest())) must be equalTo (OK)
        }

        "return result of assessmentService#getByAuthorId" in new apiScope(ids = ids, authorId = authorId) {
          contentAsJson(assessmentApi.get(authorId)(FakeRequest())) must be equalTo (Json.toJson(assessments.map(_.copy(metadata = Map("author" -> authorId.get)))))
        }

      }

    }

  }

  "update" should {

    val metadataUpdate = Map("some" -> "metadata")
    val jsonUpdate = Json.obj(
      "metadata" -> metadataUpdate)

    val updateId = new ObjectId()

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None, id = Some(updateId)) {
        status(assessmentApi.update(updateId)(FakeRequest().withJsonBody(jsonUpdate))) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "return 200" in new apiScope(id = Some(updateId)) {
        status(assessmentApi.update(updateId)(FakeRequest().withJsonBody(jsonUpdate))) must be equalTo (OK)
      }

      "call assessmentService#update" in new apiScope(id = Some(updateId)) {
        val response = contentAsJson(assessmentApi.update(updateId)(FakeRequest().withJsonBody(jsonUpdate)))
        there was one(assessmentService).update(any[Assessment])
      }

      "return updated json" in new apiScope(id = Some(updateId)) {
        val json = contentAsJson(assessmentApi.update(updateId)(FakeRequest().withJsonBody(jsonUpdate)))
        (json \ "metadata").as[Map[String, String]] must be equalTo (metadataUpdate)
      }

    }

  }

  "delete" should {

    val deleteId = new ObjectId()

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None, id = Some(deleteId)) {
        status(assessmentApi.delete(deleteId)(FakeRequest())) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "call assessmentService#delete" in new apiScope(id = Some(deleteId)) {
        Await.result(assessmentApi.delete(deleteId)(FakeRequest()), Duration.Inf)
        there was one(assessmentService).remove(assessmentFor(deleteId))
      }

      "return 200" in new apiScope(id = Some(deleteId)) {
        status(assessmentApi.delete(deleteId)(FakeRequest())) must be equalTo (OK)
      }

      "return json of deleted resource" in new apiScope(id = Some(deleteId)) {
        contentAsJson(assessmentApi.delete(deleteId)(FakeRequest())) must be equalTo (Json.toJson(assessmentFor(deleteId)))
      }

    }

  }

  "addParticipants" should {

    val assessmentId = new ObjectId()
    val participantIds = Seq("these", "are", "participant", "ids")
    val participantsJson = Json.obj(
      "ids" -> participantIds)

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None, id = Some(assessmentId)) {
        status(assessmentApi.addParticipants(assessmentId)(FakeRequest().withJsonBody(participantsJson))) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "return 200" in new apiScope(id = Some(assessmentId)) {
        status(assessmentApi.addParticipants(assessmentId)(FakeRequest().withJsonBody(participantsJson))) must be equalTo (OK)
      }

      "return assessment json containing participants with ids" in new apiScope(id = Some(assessmentId)) {
        val json = contentAsJson(assessmentApi.addParticipants(assessmentId)(FakeRequest().withJsonBody(participantsJson)))
        (json \ "participants").as[Seq[JsObject]].map(j => (j \ "externalUid").as[String]) must be equalTo (participantIds)
      }

    }

  }

  "addAnswer" should {

    val assessmentId = new ObjectId()
    val participantId = new ObjectId().toString
    val participantIds = Seq(participantId)
    val answerItemId = s"${new ObjectId()}:0"
    val answerSessionId = new ObjectId().toString
    val answerJson = Json.obj(
      "itemId" -> answerItemId,
      "sessionId" -> answerSessionId)

    "without identity" should {
      "return 401" in new apiScope(orgAndOpts = None, id = Some(assessmentId), participants = participantIds) {
        status(assessmentApi.addAnswer(assessmentId, Some(participantId))(FakeRequest().withJsonBody(answerJson))) must be equalTo (UNAUTHORIZED)
      }
    }

    "with identity" should {

      "return 200" in new apiScope(id = Some(assessmentId), participants = participantIds) {
        status(assessmentApi.addAnswer(assessmentId, Some(participantId))(FakeRequest().withJsonBody(answerJson))) must be equalTo (OK)
      }

      "return assessment json with provided answer in specified participant" in new apiScope(id = Some(assessmentId), participants = participantIds) {
        val json = contentAsJson(assessmentApi.addAnswer(assessmentId, Some(participantId))(FakeRequest().withJsonBody(answerJson)))
        (json \ "participants").as[Seq[JsObject]]
          .find(obj => (obj \ "externalUid").asOpt[String] == Some(participantId) && (obj \ "answers").as[Seq[JsObject]]
            .find(answer => (answer \ "itemId").asOpt[String] == Some(answerItemId) && (answer \ "sessionId").asOpt[String] == Some(answerSessionId)).nonEmpty) must not beEmpty
      }

    }

  }

}
