package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.CollectionExtraDetails
import org.corespring.models.auth.Permission
import play.api.libs.json._

trait CollectionExtraDetailsWrites extends Writes[CollectionExtraDetails] {

  def itemCount(id: ObjectId): Long

  def writes(c: CollectionExtraDetails): JsValue = {
    JsObject(Seq(
      "name" -> JsString(c.coll.name),
      "ownerOrgId" -> JsString(c.coll.ownerOrgId.toString),
      "permission" -> JsString(Permission.toHumanReadable(c.access)),
      "itemCount" -> JsNumber(itemCount(c.coll.id)),
      "isPublic" -> JsBoolean(c.coll.isPublic),
      "id" -> JsString(c.coll.id.toString)))
  }
}
