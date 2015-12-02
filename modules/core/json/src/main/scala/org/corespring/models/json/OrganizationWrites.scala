package org.corespring.models.json

import org.bson.types.ObjectId
import org.corespring.models.{ Organization, ContentCollRef }
import org.corespring.models.auth.Permission
import play.api.libs.json._

object CollectionReferenceWrites extends Writes[ContentCollRef] {
  def writes(ref: ContentCollRef) = {
    JsObject(
      Seq(
        "collectionId" -> JsString(ref.collectionId.toString),
        "permission" -> JsString(Permission.toHumanReadable(ref.pval)),
        "enabled" -> JsBoolean(ref.enabled)))
  }
}

abstract class BasicWrites extends Writes[Organization] {
  def writes(org: Organization) = {
    var list = List[(String, JsValue)]()
    if (org.path.nonEmpty) list = ("path" -> JsArray(org.path.map(c => JsString(c.toString)))) :: list
    if (org.name.nonEmpty) list = ("name" -> JsString(org.name)) :: list
    list = ("id" -> JsString(org.id.toString)) :: list
    JsObject(list)
  }
}

class OrganizationWrites(rootOrgId:ObjectId) extends BasicWrites {
  implicit val w: Writes[ContentCollRef] = CollectionReferenceWrites

  override def writes(org: Organization) = {
    val jsObject = super.writes(org)
    jsObject ++ Json.obj(
      "contentcolls" -> Json.toJson(org.contentcolls),
      "isRoot" -> (org.id == rootOrgId)
    )
  }
}
