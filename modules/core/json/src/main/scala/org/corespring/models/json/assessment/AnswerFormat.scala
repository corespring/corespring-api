package org.corespring.models.json.assessment

import org.bson.types.ObjectId
import org.corespring.models.assessment.Answer
import org.corespring.models.json.{ VersionedIdFormat, JsonValidationException }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json._

object AnswerFormat extends Format[Answer] {

  override def reads(json: JsValue): JsResult[Answer] = {
    JsSuccess(
      Answer(
        sessionId = new ObjectId((json \ "sessionId").as[String]),
        itemId = (json \ "itemId").asOpt[VersionedId[ObjectId]](VersionedIdFormat)
          .getOrElse(throw new JsonValidationException("You must have an item id"))))
  }

  def writes(a: Answer): JsValue = {

    JsObject(Seq(
      "sessionId" -> JsString(a.sessionId.toString),
      "itemId" -> Json.toJson(a.itemId)(VersionedIdFormat)))
    //TODO: RF: - need to glue the score/response/complete in?
    //"score" -> JsNumber(calculateScore(maybeSession)),
    //"lastResponse" -> JsNumber(getLastResponse(maybeSession)),
    //"isComplete" -> JsBoolean(isComplete(maybeSession)))

  }
}