package org.corespring.v2.player

import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.item.{ Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.{ ItemTransformerConfig, ItemTransformer }
import org.corespring.services.{ ContentCollectionService, StandardService }
import org.corespring.services.item.{ ItemService, BaseFindAndSaveService }
import org.corespring.services.errors.{ GeneralError, PlatformServiceError }
import play.api.{ Logger }

/**
 * Note: Whilst we support v1 and v2 players, we need to allow the item transformer to save 'versioned' items (aka not the most recent item).
 * This is not possible using the SalatVersioningDao as it has logic in place that prevents that happening.
 * @see SalatVersioningDao
 */

class TransformerItemService(underlying: ItemService, versionedCollection: MongoCollection, currentCollection: MongoCollection)(implicit context: Context) extends BaseFindAndSaveService[Item, VersionedId[ObjectId]] {

  private val logger = Logger(classOf[TransformerItemService])

  override def findOneById(id: VersionedId[ObjectId]): Option[Item] = underlying.findOneById(id)

  override def save(i: Item, createNewVersion: Boolean): Either[PlatformServiceError, VersionedId[ObjectId]] = {
    import com.mongodb.casbah.Imports._
    import com.novus.salat._
    logger.debug(s"function=save id=${i.id}, id=${i.id.id} version=${i.id.version}")
    val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(i))

    val collectionToSaveIn: Option[MongoCollection] = i.id.version.map { v =>
      val query = MongoDBObject("_id._id" -> i.id.id, "_id.version" -> v)

      logger.trace(s"function=collectionToSaveIn query=${com.mongodb.util.JSON.serialize(query)}")

      val idOnly = MongoDBObject("_id._id" -> i.id.id)

      logger.debug(s"function=collectionToSaveIn - find id only in versioned and current collections: ${JSON.serialize(idOnly)}")

      val versionedCount = versionedCollection.count(query)
      val currentCount = currentCollection.count(query)

      if (versionedCount == 1) {
        Some(versionedCollection)
      } else if (currentCount == 1) {
        Some(currentCollection)
      } else None
    }.flatten

    logger.trace(s"function=save id=${i.id} collection=${collectionToSaveIn}")

    collectionToSaveIn.map { c =>
      val result = c.save(dbo)
      if (result.getLastError.ok) {
        Right(i.id)
      } else {
        Left(GeneralError(result.getLastError.getErrorMessage, None))
      }
    }.getOrElse(Left(GeneralError("Can't find a collection to save in", None)))
  }
}

class AllItemVersionTransformer(
  itemService: TransformerItemService,
  contentCollectionService: ContentCollectionService,
  standardService: StandardService,
  config: ItemTransformerConfig,
  jsonFormatting: JsonFormatting)
  extends ItemTransformer(itemService, contentCollectionService, standardService, jsonFormatting, config) {

}
