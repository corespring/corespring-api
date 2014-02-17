package org.corespring.platform.core.models.assessment

import org.corespring.platform.core.models.assessment.basic.Question
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.data.mongo.models.{EntityWithVersionedId, VersionedId}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId

case class AssessmentTemplate(id: VersionedId[ObjectId] = VersionedId(ObjectId.get()),
                              orgId: Option[ObjectId] = None,
                              metadata: Map[String, String] = Map(),
                              questions: Seq[Question] = Seq()) extends EntityWithVersionedId[ObjectId]

object AssessmentTemplate extends JsonUtil {

  object Keys {
    val orgId = "orgId"
    val metadata = "metadata"
    val questions = "questions"
    val id = "id"
  }

  object Format extends Format[AssessmentTemplate] {

    import Keys._
    implicit val VersionedIdWrites = VersionedIdImplicits.Writes
    implicit val VersionedIdReads = VersionedIdImplicits.Reads
    implicit val QuestionFormat = Question.Format

    def reads(json: JsValue): JsResult[AssessmentTemplate] = {
      JsSuccess(
        AssessmentTemplate(
          id = (json \ id).as[VersionedId[ObjectId]],
          orgId = (json \ orgId).asOpt[String].map(new ObjectId(_)),
          metadata = (json \ metadata).as[Map[String, String]],
          questions = (json \ questions).as[Seq[Question]]
        )
      )
    }

    def writes(assessmentTemplate: AssessmentTemplate): JsValue = partialObj(
      id -> Some(Json.toJson(assessmentTemplate.id.toString)),
      orgId -> assessmentTemplate.orgId.map(_.toString).map(JsString(_)),
      metadata -> (assessmentTemplate.metadata match {
        case nonEmpty: Map[String, String] if nonEmpty.nonEmpty => Some(Json.toJson(nonEmpty))
        case _ => None
      }),
      questions -> Some(JsArray(assessmentTemplate.questions.map(Json.toJson(_))))
    )

  }

}