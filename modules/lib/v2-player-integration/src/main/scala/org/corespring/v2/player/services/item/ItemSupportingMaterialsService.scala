package org.corespring.v2.player.services.item

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import org.corespring.drafts.item.models.DraftId
import org.corespring.mongo.IdConverters
import org.corespring.platform.core.services.item.{SupportingMaterialsAssets, MongoSupportingMaterialsService}
import org.corespring.platform.data.mongo.models.VersionedId

class ItemSupportingMaterialsService(val collection: MongoCollection,
  val bucket: String,
  val assets: SupportingMaterialsAssets[VersionedId[ObjectId]])(implicit val ctx: Context)
  extends MongoSupportingMaterialsService[VersionedId[ObjectId]]
  with IdConverters {
  override def idToDbo(id: VersionedId[ObjectId]): DBObject = vidToDbo(id)
}

class DraftSupportingMaterialsService(val collection: MongoCollection,
  val bucket: String,
  val assets: SupportingMaterialsAssets[DraftId])(implicit val ctx: Context)
  extends MongoSupportingMaterialsService[DraftId] {
  override def idToDbo(id: DraftId): DBObject = {
    val dbo = com.novus.salat.grater[DraftId].asDBObject(id)
    MongoDBObject("_id" -> dbo)
  }

  override def prefix(s: String) = s"change.data.$s"
}
