package org.corespring.player.accessControl.models.granter
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId

trait ItemLookup {
  def containsItem(id: ObjectId, itemId: VersionedId[ObjectId]): Boolean
}
trait QuizItemLookup extends ItemLookup
trait SessionItemLookup extends ItemLookup