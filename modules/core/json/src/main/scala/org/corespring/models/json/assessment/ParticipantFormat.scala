package org.corespring.models.json.assessment

import org.corespring.models.assessment.{ Answer, Participant }
import play.api.libs.json._
import play.api.libs.json.Json.toJson

object ParticipantFormat extends Format[Participant] {

  implicit val a = AnswerFormat

  def reads(json: JsValue): JsResult[Participant] = {
    JsSuccess(new Participant(
      (json \ "answers").as[Seq[Answer]],
      (json \ "externalUid").as[String]))
  }

  def writes(p: Participant): JsValue = {
    val fields = Seq(
      "answers" -> toJson(p.answers),
      "externalUid" -> JsString(p.externalUid))
    p.lastModified match {
      case None => JsObject(fields)
      case Some(time) => JsObject(fields ++ Seq(("lastModified" -> JsNumber(time.getMillis))))
    }
  }

}
