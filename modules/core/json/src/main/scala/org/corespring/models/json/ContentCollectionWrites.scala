package org.corespring.models.json

import org.corespring.models.ContentCollection
import play.api.libs.json.{Writes, JsObject, JsString, JsValue}

object ContentCollectionWrites extends Writes[ContentCollection]{

  override def writes(coll: ContentCollection): JsValue = {
    var list = List[(String, JsString)]()
    if (coll.name.nonEmpty) list = ("name" -> JsString(coll.name)) :: list
    list = ("id" -> JsString(coll.id.toString)) :: list
    JsObject(list)
  }
}
