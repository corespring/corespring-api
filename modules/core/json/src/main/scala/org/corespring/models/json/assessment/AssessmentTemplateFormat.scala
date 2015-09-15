package org.corespring.models.json.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Question, AssessmentTemplate }
import org.corespring.models.assessment.AssessmentTemplate.Keys
import org.corespring.models.json.JsonUtil
import play.api.libs.json._

object AssessmentTemplateFormat extends Format[AssessmentTemplate] with JsonUtil {

  import Keys._
  implicit val qf = QuestionFormat

  def reads(json: JsValue): JsResult[AssessmentTemplate] = {
    JsSuccess(
      AssessmentTemplate(
        id = (json \ id).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        collectionId = (json \ collectionId).asOpt[String],
        orgId = (json \ orgId).asOpt[String].map(new ObjectId(_)),
        metadata = (json \ metadata).asOpt[Map[String, String]].getOrElse(Map.empty),
        questions = (json \ questions).asOpt[Seq[Question]].getOrElse(Seq.empty)))
  }

  def writes(assessmentTemplate: AssessmentTemplate): JsValue = partialObj(
    id -> Some(JsString(assessmentTemplate.id.toString)),
    orgId -> assessmentTemplate.orgId.map(id => JsString(id.toString)),
    collectionId -> assessmentTemplate.collectionId.map(JsString(_)),
    metadata -> (assessmentTemplate.metadata match {
      case nonEmpty: Map[String, String] if nonEmpty.nonEmpty => Some(Json.toJson(nonEmpty))
      case _ => None
    }),
    questions -> (assessmentTemplate.questions match {
      case nonEmpty: Seq[Question] if nonEmpty.nonEmpty => Some(JsArray(nonEmpty.map(Json.toJson(_))))
      case _ => None
    }))
}
