package tests.api.v1

import org.corespring.test.{BaseTest, PlaySingleton}
import org.corespring.test.utils.RequestCalling
import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.core.models.assessment.basic.{Assessment, Question}
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.{JsArray, JsObject, JsString, Json}
import org.corespring.platform.core.services.assessment.template.AssessmentTemplateServiceImpl

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

    "update metadata" in {
      updatedAssessmentTemplate.metadata must be equalTo updatedMetadata
    }

    "update questions" in {
      updatedAssessmentTemplate.questions must be equalTo updatedQuestions
    }

  }

}
