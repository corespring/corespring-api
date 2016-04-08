package org.corespring.drafts.item.models

import java.net.{ URLDecoder, URLEncoder }

import org.bson.types.ObjectId
import org.corespring.drafts.{ UserDraft }
import org.corespring.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.{ DateTimeZone, DateTime }

object ItemDraft {
  def apply(id: DraftId, item: Item, user: OrgAndUser): ItemDraft = {
    ItemDraft(
      id,
      user,
      ItemSrc(item),
      ItemSrc(item))
  }
}

/** A Draft is unique to the itemId (base id) and org and user) */
case class DraftId(itemId: ObjectId, name: String, orgId: ObjectId) {
  def toIdString = s"$itemId~${URLEncoder.encode(name, "UTF-8")}"
}

object DraftId {

  val regex = """^([^~]+)~([^~]+)$""".r

  /**
   * Parse the string of a form: objectId~username to (ObjectId,String)
   * @param s
   */
  def idStringToObjectIdAndUserName(s: String): Option[(ObjectId, String)] = {
    try {
      val regex(id, name) = s
      val withoutVersion = id.split(":")(0)
      if (ObjectId.isValid(withoutVersion)) {
        Some(new ObjectId(withoutVersion) -> URLDecoder.decode(name, "UTF-8"))
      } else {
        None
      }
    } catch {
      case t: Throwable => None
    }
  }

  def fromIdString(s: String, orgId: ObjectId): Option[DraftId] = idStringToObjectIdAndUserName(s).map {
    case (itemId, name) =>
      DraftId(itemId, name, orgId)
  }
}

case class ItemDraftHeader(id: DraftId, created: DateTime, expires: DateTime, userName: Option[String])

case class ItemDraft(
  val id: DraftId,
  val user: OrgAndUser,
  val parent: ItemSrc,
  val change: ItemSrc,
  val created: DateTime = DateTime.now(DateTimeZone.UTC),
  val expires: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1))
  extends UserDraft[DraftId, VersionedId[ObjectId], Item, OrgAndUser] {

  /**
   * Update the src data and copy over the created and expires otherwise they'll get refreshed
   */
  override def mkChange(d: Item): ItemDraft = this.copy(change = change.copy(data = d), created = this.created, expires = this.expires)

  def toHeader: ItemDraftHeader = {
    ItemDraftHeader(id, created, expires, user.user.map(_.userName))
  }
}

case class Conflict(draft: ItemDraft, item: Item)
