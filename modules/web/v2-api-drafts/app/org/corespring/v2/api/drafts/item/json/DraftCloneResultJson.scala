package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.DraftCloneResult
import play.api.libs.json.{ JsValue, Json }

object DraftCloneResultJson {
  def apply(r: DraftCloneResult): JsValue = {
    Json.obj(
      "itemId" -> r.itemId.toString,
      "draftId" -> r.draftId.toIdString)
  }
}
