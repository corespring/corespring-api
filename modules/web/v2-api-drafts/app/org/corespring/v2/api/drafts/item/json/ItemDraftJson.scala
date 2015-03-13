package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.ItemDraft
import org.corespring.platform.data.mongo.models.VersionedId
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
      "itemId" -> s"${new VersionedId(d.src.id.id, Some(d.src.id.version)).toString}",
      "user" -> d.user.userName,
      "created" -> timeJson(d.created),
      "expires" -> timeJson(d.expires))
  }
}
