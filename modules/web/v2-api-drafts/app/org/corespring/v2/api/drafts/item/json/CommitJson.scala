package org.corespring.v2.api.drafts.item.json

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ SimpleUser, ItemCommit }
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ Json, Writes, JsValue }

object CommitJson {

  implicit val oid = org.corespring.platform.core.models.json.ObjectIdWrites
  implicit val vid: Writes[VersionedId[ObjectId]] = org.corespring.platform.core.models.versioning.VersionedIdImplicits.Writes
  implicit val u: Writes[SimpleUser] = Json.writes[SimpleUser]
  implicit val f: Writes[ItemCommit] = Json.writes[ItemCommit]

  def apply(c: ItemCommit): JsValue = f.writes(c)

}
