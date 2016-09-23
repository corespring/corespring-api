package org.corespring.v2.player.services.item

import com.mongodb.casbah.Imports._
import salat.Context
import org.corespring.drafts.item.models.DraftId
import org.corespring.models.appConfig.Bucket
import org.corespring.mongo.IdConverters
import org.corespring.platform.core.services.item.{ SupportingMaterialsAssets, MongoSupportingMaterialsService }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.SupportingMaterialsService

trait ItemSupportingMaterialsService extends SupportingMaterialsService[VersionedId[ObjectId]]
trait DraftSupportingMaterialsService extends SupportingMaterialsService[DraftId]

class MongoItemSupportingMaterialsService(val collection: MongoCollection,
  val bucketConfig: Bucket,
  val assets: SupportingMaterialsAssets[VersionedId[ObjectId]])(implicit val ctx: Context)
  extends ItemSupportingMaterialsService with MongoSupportingMaterialsService[VersionedId[ObjectId]]
  with IdConverters {
  override def bucket: String = bucketConfig.bucket

  override def idToDbo(id: VersionedId[ObjectId]): DBObject = vidToDbo(id)
}

class MongoDraftSupportingMaterialsService(val collection: MongoCollection,
  val bucketConfig: Bucket,
  val assets: SupportingMaterialsAssets[DraftId])(implicit val ctx: Context)
  extends DraftSupportingMaterialsService with MongoSupportingMaterialsService[DraftId] {
  override def idToDbo(id: DraftId): DBObject = {
    val dbo = salat.grater[DraftId].asDBObject(id)
    MongoDBObject("_id" -> dbo)
  }
  override def bucket: String = bucketConfig.bucket

  override def prefix(s: String) = s"change.data.$s"
}
