package org.corespring.platform.core.models.assessment

import org.corespring.platform.core.models.assessment.basic.Question
import org.corespring.platform.core.models.JsonUtil
import play.api.libs.json._
import play.api.libs.json.JsSuccess
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.data.mongo.models.{EntityWithVersionedId, VersionedId}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.TypeImports.ObjectId
import org.corespring.platform.core.models.item.Content
import org.joda.time.DateTime
import org.corespring.platform.core.models.json.JsonValidationException

case class AssessmentTemplate(var id: VersionedId[ObjectId] = VersionedId(ObjectId.get()),
                              var collectionId: String = "",
                              orgId: Option[ObjectId] = None,
                              metadata: Map[String, String] = Map(),
                              dateModified: Option[DateTime] = Some(new DateTime()),
                              questions: Seq[Question] = Seq()) extends Content with EntityWithVersionedId[ObjectId] {
  var contentType = "assessmentTemplate"
}

object AssessmentTemplate extends JsonUtil {

  object Keys {
    val contentType = "contentType"
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
          id = (try {
            import VersionedIdImplicits.{ Reads => IdReads }
            (json \ id).asOpt[VersionedId[ObjectId]](IdReads).getOrElse(VersionedId(new ObjectId()))
          } catch {
            case e: Throwable => throw new JsonValidationException(id)
          }),
          orgId = (json \ orgId).asOpt[String].map(new ObjectId(_)),
          metadata = (json \ metadata).asOpt[Map[String, String]].getOrElse(Map.empty),
          questions = (json \ questions).asOpt[Seq[Question]].getOrElse(Seq.empty)
        )
      )
    }

    def writes(assessmentTemplate: AssessmentTemplate): JsValue = partialObj(
      id -> Some(Json.toJson(assessmentTemplate.id.toString)),
      orgId -> assessmentTemplate.orgId.map(_.toString).map(JsString(_)),
      contentType -> Some(JsString(assessmentTemplate.contentType)),
      metadata -> (assessmentTemplate.metadata match {
        case nonEmpty: Map[String, String] if nonEmpty.nonEmpty => Some(Json.toJson(nonEmpty))
        case _ => None
      }),
      questions -> Some(JsArray(assessmentTemplate.questions.map(Json.toJson(_))))
    )

  }

}