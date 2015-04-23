package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.corespring.drafts.{ Src }
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId

case class ItemSrc(data: Item)
  extends Src[VersionedId[ObjectId], Item] {

  require(data != null, "data must not be null")

  override protected def dataWithVid: HasVid[VersionedId[ObjectId]] = {
    new HasVid[VersionedId[ObjectId]] {
      override def id: VersionedId[ObjectId] = {
        println(s"data: $data")
        data.id
      }
    }
  }
}
