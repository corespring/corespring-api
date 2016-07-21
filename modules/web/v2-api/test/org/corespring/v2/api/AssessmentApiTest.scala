package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Answer, Assessment, Participant }
import org.corespring.services.assessment.AssessmentService
import org.corespring.v2.actions.V2ActionsFactory
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, Json }
import play.api.test.FakeRequest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class AssessmentApiTest extends V2ApiSpec {

  import jsonFormatting._

  class apiScope(
    id: Option[ObjectId] = None,
    ids: List[ObjectId] = List.empty[ObjectId],
    authorId: Option[String] = None,
    participants: Seq[String] = Seq.empty[String]) extends Scope with V2ApiScope {

    lazy val v2Actions = V2ActionsFactory.apply()
    lazy val orgAndOpts = V2ActionsFactory.orgAndOpts

    val orgId = Some(orgAndOpts.org.id)

    def assessmentFor(id: ObjectId) =
      new Assessment(id = id, orgId = orgId, participants = participants.map(id => Participant(Seq(), id)))

    val assessments = ids.map(id => assessmentFor(id))

    val allIds = id match {
      case Some(i) => ids :+ i
      case _ => ids
    }

    val assessmentService = {
      val m = mock[AssessmentService]
      orgId match {
        case Some(orgId) =>
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

    val assessmentApi = new AssessmentApi(
      v2Actions,
      assessmentService,
      jsonFormatting,
      v2ApiContext)

  }

  private val fr = FakeRequest()

  "create" should {

    class create extends apiScope() {
      val result = assessmentApi.create()(fr)
      val resultJson = contentAsJson(result)
      val resultStatus = status(result)
    }

    "call assessmentService#create" in new create {
      Await.result(result, Duration.Inf)
      there was one(assessmentService).create(any[Assessment])
    }

    "return 201" in new create {
      resultStatus === CREATED
    }

    "return created Assessment id" in new create {
      (resultJson \ "id").asOpt[String] must not beEmpty
    }

    "return created Assessment orgId" in new create {
      (resultJson \ "orgId").asOpt[String] === Some(orgAndOpts.org.id.toString)
    }
  }

  "getByIds" should {

    val ids = List(new ObjectId(), new ObjectId())

    class getByIds(ids: List[ObjectId]) extends apiScope(ids = ids) {
      val result = assessmentApi.getByIds(ids.map(_.toString).mkString(","))(fr)
      lazy val resultJson = contentAsJson(result)
      lazy val resultStatus = status(result)
    }

    "return 200" in new getByIds(ids = ids) {
      resultStatus === OK
    }

    "return JSON for Assessments with each id" in new getByIds(ids = ids) {
      (resultJson \\ "id").map(_.as[String]) === ids.map(_.toString)
    }

    "with a single id" should {

      "return 200" in new getByIds(ids = ids.take(1)) {
        resultStatus === OK
      }

      "return single json object for id" in new getByIds(ids = ids.take(1)) {
        (resultJson.as[JsObject] \ "id").as[String] === ids.head.toString
      }
    }
  }

  "get" should {

    val ids = List(new ObjectId(), new ObjectId())
    val authorId = Some("abc123")

    "return 200" in new apiScope(ids = ids) {
      status(assessmentApi.get(None)(fr)) === OK
    }

    "return result of assessmentService#getAllByOrgId" in new apiScope(ids = ids) {
      val json = contentAsJson(assessmentApi.get(None)(fr))
      there was one(assessmentService).findAllByOrgId(orgId.get)
      json === Json.toJson(assessments)
    }

    "with authorId" should {

      "return 200" in new apiScope(ids = ids, authorId = authorId) {
        status(assessmentApi.get(authorId)(fr)) === OK
      }

      "return result of assessmentService#getByAuthorId" in new apiScope(ids = ids, authorId = authorId) {
        contentAsJson(assessmentApi.get(authorId)(fr)) === Json.toJson(assessments.map(_.copy(metadata = Map("author" -> authorId.get))))
      }
    }
  }

  "update" should {

    val metadataUpdate = Map("some" -> "metadata")
    val jsonUpdate = Json.obj(
      "metadata" -> metadataUpdate)

    val updateId = new ObjectId()

    "return 200" in new apiScope(id = Some(updateId)) {
      status(assessmentApi.update(updateId)(fr.withJsonBody(jsonUpdate))) === OK
    }

    "call assessmentService#update" in new apiScope(id = Some(updateId)) {
      val response = contentAsJson(assessmentApi.update(updateId)(fr.withJsonBody(jsonUpdate)))
      there was one(assessmentService).update(any[Assessment])
    }

    "return updated json" in new apiScope(id = Some(updateId)) {
      val json = contentAsJson(assessmentApi.update(updateId)(fr.withJsonBody(jsonUpdate)))
      (json \ "metadata").as[Map[String, String]] === metadataUpdate
    }
  }

  "delete" should {

    val deleteId = new ObjectId()

    "call assessmentService#delete" in new apiScope(id = Some(deleteId)) {
      Await.result(assessmentApi.delete(deleteId)(fr), Duration.Inf)
      there was one(assessmentService).remove(assessmentFor(deleteId))
    }

    "return 200" in new apiScope(id = Some(deleteId)) {
      status(assessmentApi.delete(deleteId)(fr)) === OK
    }

    "return json of deleted resource" in new apiScope(id = Some(deleteId)) {
      contentAsJson(assessmentApi.delete(deleteId)(fr)) === Json.toJson(assessmentFor(deleteId))
    }
  }

  "addParticipants" should {

    val assessmentId = new ObjectId()
    val participantIds = Seq("these", "are", "participant", "ids")
    val participantsJson = Json.obj(
      "ids" -> participantIds)

    "return 200" in new apiScope(id = Some(assessmentId)) {
      status(assessmentApi.addParticipants(assessmentId)(fr.withJsonBody(participantsJson))) === OK
    }

    "return assessment json containing participants with ids" in new apiScope(id = Some(assessmentId)) {
      val json = contentAsJson(assessmentApi.addParticipants(assessmentId)(fr.withJsonBody(participantsJson)))
      (json \ "participants").as[Seq[JsObject]].map(j => (j \ "externalUid").as[String]) === participantIds
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

    "return 200" in new apiScope(id = Some(assessmentId), participants = participantIds) {
      status(assessmentApi.addAnswer(assessmentId, Some(participantId))(fr.withJsonBody(answerJson))) === OK
    }

    "return assessment json with provided answer in specified participant" in new apiScope(id = Some(assessmentId), participants = participantIds) {
      val json = contentAsJson(assessmentApi.addAnswer(assessmentId, Some(participantId))(fr.withJsonBody(answerJson)))

      (json \ "participants").as[Seq[JsObject]]
        .exists(obj => (obj \ "externalUid").asOpt[String] == Some(participantId) && (obj \ "answers").as[Seq[JsObject]]
          .exists(answer => (answer \ "itemId").asOpt[String] == Some(answerItemId) && (answer \ "sessionId").asOpt[String] == Some(answerSessionId)).nonEmpty) === true
    }
  }

}
