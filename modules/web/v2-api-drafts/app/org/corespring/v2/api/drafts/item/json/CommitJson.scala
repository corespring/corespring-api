package org.corespring.v2.api.drafts.item.json

import org.corespring.drafts.item.models.{ SimpleUser, ItemCommit }
import play.api.libs.json.{ Json, Writes, JsValue }

object CommitJson {

  implicit val vid = org.corespring.platform.core.models.versioning.VersionedIdImplicits.Writes
  implicit val u: Writes[SimpleUser] = Json.format[SimpleUser]
  val f: Writes[ItemCommit] = Json.writes[ItemCommit]

  def apply(c: ItemCommit): JsValue = f.writes(c)

}
