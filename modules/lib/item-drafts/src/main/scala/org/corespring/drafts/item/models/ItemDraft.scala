package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.{ Src, UserDraft }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.{ DateTimeZone, DateTime }

object ItemDraft {
  def apply(item: Item, user: OrgAndUser): ItemDraft = {
    ItemDraft(
      DraftId(item.id.id, user.user.map { _.userName }.getOrElse("unknown_user"), user.org.id),
      user,
      ItemSrc(item),
      ItemSrc(item),
      false)
  }
}

/** A Draft is unique to the itemId (base id) and org and user) */
case class DraftId(itemId: ObjectId, name: String, orgId: ObjectId)

case class ItemDraft(
  val id: DraftId,
  val user: OrgAndUser,
  val src: ItemSrc,
  val change: ItemSrc,
  val hasConflict: Boolean,
  val created: DateTime = DateTime.now(DateTimeZone.UTC),
  val expires: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1))
  extends UserDraft[ObjectId, VersionedId[ObjectId], Item, OrgAndUser] {

  /**
   * Update the src data and copy over the created and expires otherwise they'll get refreshed
   */
  override def mkChange(d: Item): ItemDraft = this.copy(change = change.copy(data = d), created = this.created, expires = this.expires)

}
