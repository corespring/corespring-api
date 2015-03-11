package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.UserDraft
import org.corespring.platform.core.models.item.Item

case class ItemDraft(
  val id: ObjectId,
  override val src: ItemSrc,
  override val user: SimpleUser)
  extends UserDraft[ObjectId, ObjectId, Long, Item, SimpleUser] {
  override def update(d: Item): ItemDraft = this.copy(src = src.copy(data = d))
}
