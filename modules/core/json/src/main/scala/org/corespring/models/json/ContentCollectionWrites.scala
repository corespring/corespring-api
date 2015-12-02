package org.corespring.models.json

import org.corespring.models.{ CollectionInfo, ContentCollection }
import play.api.libs.json._

object ContentCollectionWrites extends Writes[ContentCollection] {

  private implicit val oidFormat = ObjectIdFormat

  private val defaultWrites = Json.writes[ContentCollection]
  override def writes(coll: ContentCollection): JsValue = defaultWrites.writes(coll)
}

object CollectionInfoWrites extends Writes[CollectionInfo] {

  private implicit val w = ContentCollectionWrites

  override def writes(wc: CollectionInfo): JsValue = {
    Json.obj(
      "itemCount" -> wc.itemCount,
      "permission" -> wc.orgPermission.name.toLowerCase)
      .deepMerge(
        Json.toJson(wc.contentCollection).as[JsObject])
  }
}
