package tests.api.v1

import org.corespring.test.{BaseTest, PlaySingleton}
import org.corespring.test.utils.RequestCalling
import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.core.models.assessment.basic.{Assessment, Question}
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson}
import play.api.libs.json._
import org.corespring.platform.core.services.assessment.template.AssessmentTemplateServiceImpl
import play.api.libs.json.JsArray
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some

class AssessmentTemplateApiTest extends BaseTest with RequestCalling {

  PlaySingleton.start()
  val Api = api.v1.AssessmentTemplateApi

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  val metadata = Map("course" -> "some course")
  val questionIds = Seq(VersionedId(new ObjectId()))

  "create" should {
    val assessmentTemplate = AssessmentTemplate(
      collectionId = Some(TEST_COLLECTION_ID),
      metadata = metadata,
      questions = questionIds.map(itemId => Question(itemId = itemId))
    )

    val savedAssessmentTemplate = invokeCall[AssessmentTemplate](Api.create(), AnyContentAsJson(Json.toJson(assessmentTemplate)))

    "assign id to assessment template" in {
      savedAssessmentTemplate.id must not be equalTo("")
    }

    "set collection id" in {
      savedAssessmentTemplate.collectionId must be equalTo assessmentTemplate.collectionId
    }

    "set organization id" in {
      savedAssessmentTemplate.orgId must not beEmpty
    }

    "persist questions" in {
      (savedAssessmentTemplate.questions diff assessmentTemplate.questions) must beEmpty
    }

    "persist metadata" in {
      savedAssessmentTemplate.metadata must be equalTo(assessmentTemplate.metadata)
    }

  }

  "update" should {

    implicit val QuestionFormat = Question.Format

    val assessmentTemplate = AssessmentTemplate(
      collectionId = Some(TEST_COLLECTION_ID),
      metadata = metadata,
      questions = questionIds.map(itemId => Question(itemId = itemId))
    )

    val updatedMetadata = Map("course" -> "a new course")
    val updatedQuestionIds = Seq(VersionedId(new ObjectId()))
    val updatedQuestions = updatedQuestionIds.map(itemId => Question(itemId = itemId))

    val savedAssessmentTemplate =
      invokeCall[AssessmentTemplate](Api.create(), AnyContentAsJson(Json.toJson(assessmentTemplate)))
    val persistedId = savedAssessmentTemplate.id

    val assessmentTemplateUpdates = Json.obj(
      "metadata" -> JsObject(updatedMetadata.map{case (k,v) => (k -> JsString(v))}.iterator.toSeq),
      "questions" -> JsArray(updatedQuestions.map(Json.toJson(_)))
    )

    val updatedAssessmentTemplate =
      invokeCall[AssessmentTemplate](Api.update(persistedId), AnyContentAsJson(assessmentTemplateUpdates))

    "not change assessment template id" in {
      updatedAssessmentTemplate.id must be equalTo persistedId
    }

    "preserve collection id" in {
      updatedAssessmentTemplate.collectionId must be equalTo savedAssessmentTemplate.collectionId
    }

    "preserve organization id" in {
      updatedAssessmentTemplate.orgId must be equalTo savedAssessmentTemplate.orgId
    }

    "update metadata" in {
      updatedAssessmentTemplate.metadata must be equalTo updatedMetadata
    }

    "update questions" in {
      updatedAssessmentTemplate.questions must be equalTo updatedQuestions
    }

  }

  "list" should {

    implicit val QuestionFormat = Question.Format

    val assessmentTemplate = AssessmentTemplate(
      collectionId = Some(TEST_COLLECTION_ID),
      metadata = metadata,
      questions = questionIds.map(itemId => Question(itemId = itemId))
    )

    val savedAssessmentTemplate =
      invokeCall[AssessmentTemplate](Api.create(), AnyContentAsJson(Json.toJson(assessmentTemplate)))
    val persistedId = savedAssessmentTemplate.id

    val response = invokeCall[Seq[AssessmentTemplate]](Api.list(None, None, "false", 0, 0, None), AnyContentAsEmpty)

    "return persisted assessment template in response" in {
      response.find(_.id == persistedId) must not beEmpty
    }
  }

  "listAndCount" should {

    case class ListAndCountResponse(data: Seq[AssessmentTemplate], count: Int)

    implicit object Format extends Format[ListAndCountResponse] {
      implicit val AssessmentTemplateFormat = AssessmentTemplate.Format
      def reads(json: JsValue): JsResult[ListAndCountResponse] = json match {
        case jsObject: JsObject => JsSuccess(
          ListAndCountResponse(
            data = (Json.fromJson[Seq[AssessmentTemplate]](jsObject \ "data") match {
              case JsSuccess(templates: Seq[AssessmentTemplate], _) => templates
              case _ => Seq.empty
            }),
            count = (jsObject \ "count").as[Int]
          )
        )
        case _ => throw new Exception("Bad response format")
      }

      def writes(listAndCountResponse: ListAndCountResponse) = Json.obj(
        "data" -> Json.toJson(listAndCountResponse.data),
        "count" -> JsNumber(listAndCountResponse.count)
      )
    }

    val assessmentTemplate = AssessmentTemplate(
      collectionId = Some(TEST_COLLECTION_ID),
      metadata = metadata,
      questions = questionIds.map(itemId => Question(itemId = itemId))
    )

    val savedAssessmentTemplate =
      invokeCall[AssessmentTemplate](Api.create(), AnyContentAsJson(Json.toJson(assessmentTemplate)))
    val persistedId = savedAssessmentTemplate.id

    val response = invokeCall[ListAndCountResponse](Api.listAndCount(None, None, 0, 0, None), AnyContentAsEmpty)

    "return persisted assesment template in response" in {
      response.data.find(_.id == persistedId) must not beEmpty
    }

    "return count of assessment templates in response" in {
      response.count must not be equalTo(0)
    }

  }

}
