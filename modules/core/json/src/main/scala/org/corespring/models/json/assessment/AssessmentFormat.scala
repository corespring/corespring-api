package org.corespring.models.json.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.{ Question, Participant, Assessment }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Json.toJson

object AssessmentFormat extends Format[Assessment] {

  implicit val pf = ParticipantFormat
  implicit val qf = QuestionFormat

  def writes(q: Assessment): JsObject = {

    val props = List(
      Some("id" -> JsString(q.id.toString)),
      q.orgId.map((o: ObjectId) => ("orgId" -> JsString(o.toString))),
      Some("metadata" -> toJson(q.metadata)),
      Some("start" -> toJson(q.starts)),
      Some("end" -> toJson(q.ends)),
      Some("participants" -> toJson(q.participants)),
      Some("questions" -> toJson(q.questions))).flatten

    JsObject(props)
  }

  def reads(json: JsValue): JsResult[Assessment] = {

    val participants = (json \ "participants").asOpt[Seq[Participant]].getOrElse(Seq())

    JsSuccess(Assessment(
      (json \ "orgId").asOpt[String].map(new ObjectId(_)),
      (json \ "metadata").asOpt[Map[String, String]].getOrElse(Map()),
      (json \ "questions").asOpt[Seq[Question]].getOrElse(Seq()),
      (json \ "start").asOpt[DateTime],
      (json \ "end").asOpt[DateTime],
      participants,
      (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId())))
  }
}
