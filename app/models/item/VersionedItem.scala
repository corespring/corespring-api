package models.item

import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.{Holder, VersionedId}

case class VersionedItem(id:VersionedId[ObjectId], entity : Item) extends Holder[Item]


