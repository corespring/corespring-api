package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.UserDraft
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.joda.time.{ DateTimeZone, DateTime }

object ItemDraft {
  def apply(item: Item, user: OrgAndUser): ItemDraft = {
    ItemDraft(
      ObjectId.get,
      ItemSrc(item),
      user)
  }
}

case class ItemDraft(
  val id: ObjectId,
  val src: ItemSrc,
  val user: OrgAndUser,
  val created: DateTime = DateTime.now(DateTimeZone.UTC),
  val expires: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1))
  extends UserDraft[ObjectId, VersionedId[ObjectId], Item, OrgAndUser] {

  /**
   * Update the src data and copy over the created and expires otherwise they'll get refreshed
   */
  override def update(d: Item): ItemDraft = this.copy(src = src.copy(data = d), created = this.created, expires = this.expires)

}
