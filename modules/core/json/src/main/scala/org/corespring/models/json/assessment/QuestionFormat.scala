package org.corespring.models.json.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.Question
import org.corespring.models.itemSession.ItemSessionSettings
import org.corespring.models.json.VersionedIdFormat
import org.corespring.models.json.itemSession.ItemSessionSettingsFormat
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._
import play.api.libs.json.Json.toJson

object QuestionFormat extends Format[Question] {

  implicit val is = ItemSessionSettingsFormat

  def reads(json: JsValue): JsResult[Question] = {

    JsSuccess(Question(
      (json \ "itemId").as[VersionedId[ObjectId]](VersionedIdFormat),
      (json \ "settings").asOpt[ItemSessionSettings].getOrElse(ItemSessionSettings())))
  }

  def writes(q: Question): JsValue = {
    JsObject(
      Seq(
        Some("itemId" -> toJson(q.itemId)(VersionedIdFormat)),
        if (q.settings != null) Some("settings" -> toJson(q.settings)) else None,
        q.title.map("title" -> JsString(_)),
        Some("standards" -> JsArray(q.standards.map(JsString(_))))).flatten)
  }
}
