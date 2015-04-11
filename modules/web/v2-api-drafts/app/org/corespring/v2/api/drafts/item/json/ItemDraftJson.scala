package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.ItemDraft
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.json.ContentView
import org.corespring.platform.core.models.json.ItemView
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.{ DateTimeZone, DateTime }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{ JsObject, JsValue, Json }

object ItemDraftJson {

  lazy val longDateTime = DateTimeFormat.longDateTime()

  private def timeJson(d: DateTime): JsValue = Json.obj(
    "readable" -> longDateTime.print(d.withZone(DateTimeZone.UTC)),
    "timestamp" -> d.getMillis())

  def simple(d: ItemDraft): JsValue = {
    Json.obj(
      "id" -> d.id.toString,
      "itemId" -> s"${d.src.id.toString}",
      "orgId" -> d.user.org.id.toString,
      "created" -> timeJson(d.created),
      "expires" -> timeJson(d.expires)) ++
      d.user.user
      .map(u => Json.obj("user" -> u.userName))
      .getOrElse(Json.obj())

  }

  def withFullItem(d: ItemDraft): JsValue = {

    import ItemView.Writes
    val itemJson = Json.toJson[ContentView[Item]](ContentView(d.src.data, None))
    val simpleJson = simple(d)
    simpleJson.asInstanceOf[JsObject] ++ Json.obj("src" -> Json.obj("data" -> itemJson))
  }

}