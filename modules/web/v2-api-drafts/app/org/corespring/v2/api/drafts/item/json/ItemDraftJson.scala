package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.ItemDraft
import org.joda.time.{ DateTimeZone, DateTime }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{ JsValue, Json }

object ItemDraftJson {

  lazy val longDateTime = DateTimeFormat.longDateTime()

  private def timeJson(d: DateTime): JsValue = Json.obj(
    "readable" -> longDateTime.print(d.withZone(DateTimeZone.UTC)),
    "timestamp" -> d.getMillis())

  def simple(d: ItemDraft): JsValue = {
    Json.obj(
      "id" -> d.id.toString,
      "itemId" -> s"${d.src.id}:${d.src.id.version}",
      "created" -> timeJson(d.created),
      "expires" -> timeJson(d.expires))
  }
}
