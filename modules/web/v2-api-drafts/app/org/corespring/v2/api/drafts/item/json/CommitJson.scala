package org.corespring.v2.api.drafts.item.json

import org.bson.types.ObjectId
import org.corespring.drafts.item.models._
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.json.{ Json, Writes, JsValue }

object CommitJson {

  implicit val oid = org.corespring.models.json.ObjectIdFormat
  implicit val vid: Writes[VersionedId[ObjectId]] = org.corespring.models.json.VersionedIdFormat
  implicit val did: Writes[DraftId] = Json.writes[DraftId]
  implicit val u: Writes[SimpleUser] = Json.writes[SimpleUser]
  implicit val su: Writes[SimpleOrg] = Json.writes[SimpleOrg]
  implicit val ou: Writes[OrgAndUser] = Json.writes[OrgAndUser]
  implicit val f: Writes[ItemCommit] = Json.writes[ItemCommit]

  def apply(c: ItemCommit): JsValue = f.writes(c)

}
