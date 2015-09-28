package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.{ Conflict, ItemDraft, ItemDraftHeader }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.json.ContentView
import org.corespring.platform.core.models.json.ItemView
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json.{ JsObject, JsValue, Json }

object ItemDraftJson {

  lazy val longDateTime = DateTimeFormat.longDateTime()

  private def timeJson(d: DateTime): JsValue = Json.obj(
    "readable" -> longDateTime.print(d.withZone(DateTimeZone.UTC)),
    "timestamp" -> d.getMillis())

  def header(h: ItemDraftHeader): JsValue = {
    Json.obj(
      "id" -> h.id.toString,
      "itemId" -> s"${h.id.itemId.toString}",
      "orgId" -> h.id.orgId.toString,
      "created" -> timeJson(h.created),
      "expires" -> timeJson(h.expires)) ++
      h.userName
      .map(n => Json.obj("user" -> n))
      .getOrElse(Json.obj())
  }

  def withFullItem(d: ItemDraft): JsValue = {

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

    import ItemView.Writes
    val itemJson = Json.toJson[ContentView[Item]](ContentView(d.parent.data, None))
    val simpleJson = simple(d)
    simpleJson.asInstanceOf[JsObject] ++ Json.obj("src" -> Json.obj("data" -> itemJson))
  }

  def conflict(c: Conflict): JsValue = {
    Json.obj(
      "draftId" -> c.draft.id.toIdString,
      "draftParent" -> Json.toJson(c.draft.parent.data),
      "draftChange" -> Json.toJson(c.draft.change.data),
      "item" -> Json.toJson(c.item))
  }

}
