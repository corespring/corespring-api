package org.corespring.drafts

import org.corespring.drafts.item.services.ItemDraftDbUtils
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

import scalaz.{ Failure, Success }

class LoadOrCreateExpiredDraftIntegrationTest extends ItemDraftsIntegrationSpecification with ItemDraftDbUtils {

  override val context = main.context

  "loadOrCreate with expired draft" should {
    "overwrite the expired draft when loadingOrCreating" in new orgAndUserAndItem {

      val draftId = draftIdFromItemIdAndUser(itemId, orgAndUser)
      drafts.loadOrCreate(orgAndUser)(draftId)

      val loadedDraft = drafts.load(orgAndUser)(draftId)

      logger.trace(s">> loadedDraft: $loadedDraft")
      logger.trace(s">> expires: ${loadedDraft.toOption.get.expires}")
      val collection = main.db.getCollection("drafts.items")
      val update: DBObject = $set("expires" -> DateTime.now.minusHours(1))

      //set the expires so that the item will no longer load
      collection.findAndModify(
        idToDbo(draftId),
        MongoDBObject.empty,
        MongoDBObject.empty,
        false,
        update,
        true,
        false)

      val secondLoadedDraft = drafts.load(orgAndUser)(draftId)

      //the draft has expired so we can't load it any more
      secondLoadedDraft.isFailure must_== true

      logger.trace(s">> secondLoadedDraft: $secondLoadedDraft")

      drafts.loadOrCreate(orgAndUser)(draftId) match {
        case Failure(e) => ko("shouldnt be getting an error.")
        case Success(draft) => {
          logger.trace(s"overwritten draft expires: ${draft.expires}")
          draft.expires.isAfter(DateTime.now) must_== true
        }
      }
    }
  }
}
