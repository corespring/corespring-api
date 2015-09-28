package org.corespring.drafts.item

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.item.models._
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

trait ItemDraftHelper {

  implicit def context: Context

  def collection: MongoCollection = Play.current.plugin[SalatPlugin].map(_.db()("drafts.items")).get

  def create(id: DraftId, itemId: VersionedId[ObjectId], org: Organization): DraftId = {
    ItemServiceWired.findOneById(itemId).map { item =>
      val draft = ItemDraft(id, item, OrgAndUser(SimpleOrg(org.id, org.name), None))
      val dbo = com.novus.salat.grater[ItemDraft].asDBObject(draft)
      collection.insert(dbo)
      draft.id
    }.getOrElse { throw new RuntimeException(s"no item with id $itemId") }
  }

  def delete(id: DraftId) = {
    println(s"[ItemDraftHelper] delete: $id")
    collection.remove(MongoDBObject("_id.itemId" -> id.itemId))
  }

  def get(id: DraftId): Option[ItemDraft] = {
    collection
      .findOne(MongoDBObject("_id.itemId" -> id.itemId))
      .map { dbo =>
        val out = com.novus.salat.grater[ItemDraft].asObject(dbo)
        println(s"[ItemDraftHelper] draft: $out")
        out
      }
  }
}
