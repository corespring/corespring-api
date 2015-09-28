package org.corespring.drafts.item

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.drafts.item.models._
import org.corespring.drafts.item.services.ItemDraftConfig
import org.corespring.models.Organization
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import play.api.{ Logger, Play }
import se.radley.plugin.salat.SalatPlugin

trait ItemDraftHelper {

  protected val logger = Logger(classOf[ItemDraftHelper])

  implicit def context: Context

  def collection: MongoCollection = Play.current.plugin[SalatPlugin].map(_.db()(ItemDraftConfig.CollectionNames.itemDrafts)).get

  def itemService: ItemService
  def create(id: DraftId, itemId: VersionedId[ObjectId], org: Organization): DraftId = {
    itemService.findOneById(itemId).map { item =>
      val draft = ItemDraft(id, item, OrgAndUser(SimpleOrg(org.id, org.name), None))
      val dbo = com.novus.salat.grater[ItemDraft].asDBObject(draft)

      logger.debug(s"function=create, collection=${collection.name}")
      logger.trace(s"function=create, dbo=$dbo")
      collection.insert(dbo)
      draft.id
    }.getOrElse { throw new RuntimeException(s"no item with id $itemId") }
  }

  def delete(id: DraftId) = {
    logger.debug(s"[ItemDraftHelper] delete: $id")
    collection.remove(MongoDBObject("_id.itemId" -> id.itemId))
  }

  def get(id: DraftId): Option[ItemDraft] = {
    collection
      .findOne(MongoDBObject("_id.itemId" -> id.itemId))
      .map { dbo =>
        val out = com.novus.salat.grater[ItemDraft].asObject(dbo)
        out
      }
  }
}
