package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.UserDraft
import org.corespring.platform.core.models.item.Item
import org.joda.time.{ DateTimeZone, DateTime }

case class ItemDraft(
  val id: ObjectId,
  val src: ItemSrc,
  val user: SimpleUser,
  val created: DateTime = DateTime.now(DateTimeZone.UTC),
  val expires: DateTime = DateTime.now(DateTimeZone.UTC).plusDays(1))
  extends UserDraft[ObjectId, ObjectId, Long, Item, SimpleUser] {
  /**
   * Update the src data and copy over the created and expires otherwise they'll get refreshed
   */
  override def update(d: Item): ItemDraft = this.copy(src = src.copy(data = d), created = this.created, expires = this.expires)

}
