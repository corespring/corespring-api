package player.accessControl.models.granter

import org.bson.types.ObjectId

trait ItemLookup {
  def containsItem(id: ObjectId, itemId: ObjectId): Boolean
}

trait QuizItemLookup extends ItemLookup

trait SessionItemLookup extends ItemLookup
