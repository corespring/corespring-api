package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.Src
import org.corespring.platform.core.models.item.Item

case class ItemSrc(data: Item, id: ObjectIdAndVersion) extends Src[Item, ObjectId, Long]
