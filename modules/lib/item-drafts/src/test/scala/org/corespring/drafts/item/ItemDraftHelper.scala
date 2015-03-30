package org.corespring.drafts.item

import com.mongodb.casbah.MongoCollection
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.drafts.item.models._
import org.corespring.platform.core.models.{ Organization, User }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play
import se.radley.plugin.salat.SalatPlugin

trait ItemDraftHelper {

  implicit def context: Context

  def collection: MongoCollection = Play.current.plugin[SalatPlugin].map(_.db()("drafts.items")).get

  def create(itemId: VersionedId[ObjectId], org: Organization): ObjectId = {
    ItemServiceWired.findOneById(itemId).map { item =>
      val draft = ItemDraft(item, OrgAndUser(SimpleOrg(org.id, org.name), None))
      val dbo = com.novus.salat.grater[ItemDraft].asDBObject(draft)
      collection.insert(dbo)
      draft.id
    }.getOrElse { throw new RuntimeException(s"no item with id $itemId") }
  }
}
