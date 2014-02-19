package tests.api.v1

import org.corespring.test.{BaseTest, PlaySingleton}
import org.corespring.test.utils.RequestCalling
import org.corespring.platform.core.models.assessment.AssessmentTemplate
import org.corespring.platform.core.models.assessment.basic.{Assessment, Question}
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.Json

class AssessmentTemplateApiTest extends BaseTest with RequestCalling {

  PlaySingleton.start()
  val Api = api.v1.AssessmentTemplateApi

  implicit val AssessmentTemplateFormat = AssessmentTemplate.Format

  "create" in {
    val questionIds = Seq(VersionedId(new ObjectId()))
    val assessmentTemplate = AssessmentTemplate(
      collectionId = Some(TEST_COLLECTION_ID),
      metadata = Map("course" -> "some course"),
      questions = questionIds.map(itemId => Question(itemId = itemId))
    )

    val savedAssessmentTemplate = invokeCall[AssessmentTemplate](Api.create(), AnyContentAsJson(Json.toJson(assessmentTemplate)))

    savedAssessmentTemplate.id must not be equalTo("")
    savedAssessmentTemplate.collectionId must be equalTo assessmentTemplate.collectionId
    (savedAssessmentTemplate.questions diff assessmentTemplate.questions) must beEmpty
    savedAssessmentTemplate.metadata must be equalTo(assessmentTemplate.metadata)
  }

}
