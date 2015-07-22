package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.{ Conflict, ItemDraft }
import org.corespring.models.json.JsonFormatting
import org.joda.time.{ DateTimeZone, DateTime }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{ JsObject, JsValue, Json }

trait ItemDraftJson {

  def jsonFormatting: JsonFormatting

  lazy val longDateTime = DateTimeFormat.longDateTime()

  private def timeJson(d: DateTime): JsValue = Json.obj(
    "readable" -> longDateTime.print(d.withZone(DateTimeZone.UTC)),
    "timestamp" -> d.getMillis())

  def simple(d: ItemDraft): JsValue = {
    Json.obj(
      "id" -> d.id.toString,
      "itemId" -> s"${d.parent.id.toString}",
      "orgId" -> d.user.org.id.toString,
      "created" -> timeJson(d.created),
      "expires" -> timeJson(d.expires)) ++
      d.user.user
      .map(u => Json.obj("user" -> u.userName))
      .getOrElse(Json.obj())

  }

  def withFullItem(d: ItemDraft): JsValue = {
    val itemJson = jsonFormatting.item.writes(d.parent.data)
    val simpleJson = simple(d)
    simpleJson.asInstanceOf[JsObject] ++ Json.obj("src" -> Json.obj("data" -> itemJson))
  }

  def conflict(c: Conflict): JsValue = {

    implicit val i = jsonFormatting.item
    Json.obj(
      "draftId" -> c.draft.id.toIdString,
      "draftParent" -> Json.toJson(c.draft.parent.data),
      "draftChange" -> Json.toJson(c.draft.change.data),
      "item" -> Json.toJson(c.item))
  }

}
