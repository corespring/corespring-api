package org.corespring.v2.player

import com.mongodb.casbah.Imports
import com.mongodb.util.JSON
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayItemTransformationCache }
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.api.Logger
import play.api.libs.json.Json

/**
 * Note: Whilst we support v1 and v2 players, we need to allow the item transformer to save 'versioned' items (aka not the most recent item).
 * This is not possible using the SalatVersioningDao as it has logic in place that prevents that happening.
 * @see SalatVersioningDao
 */
class AllItemVersionTransformer extends ItemTransformer {

  override lazy val logger = Logger("org.corespring.v2.player.AllItemsVersionTransformer")

  def cache: ItemTransformationCache = PlayItemTransformationCache

  def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]] = new BaseFindAndSaveService[Item, VersionedId[ObjectId]] {

    override def findOneById(id: VersionedId[Imports.ObjectId]): Option[Item] = ItemServiceWired.findOneById(id)

    override def save(i: Item, createNewVersion: Boolean): Unit = {
      import com.mongodb.casbah.Imports._
      import com.novus.salat._
      import org.corespring.platform.core.models.mongoContext.context
      logger.debug(s"function=save id=${i.id}, id=${i.id.id} version=${i.id.version}")
      val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(i))

      def collectionToSaveIn(): Option[MongoCollection] = i.id.version.map { v =>
        val query = MongoDBObject("_id._id" -> i.id.id, "_id.version" -> v)

        logger.trace(s"function=collectionToSaveIn query=${com.mongodb.util.JSON.serialize(query)}")

        val idOnly = MongoDBObject("_id._id" -> i.id.id)

        logger.debug(s"function=collectionToSaveIn - find id only in versioned and current collections: ${JSON.serialize(idOnly)}")

        val versionedCount = ItemServiceWired.dao.versionedCollection.count(query)
        val currentCount = ItemServiceWired.dao.currentCollection.count(query)

        if (versionedCount == 1) {
          Some(ItemServiceWired.dao.versionedCollection)
        } else if (currentCount == 1) {
          Some(ItemServiceWired.dao.currentCollection)
        } else None
      }.flatten

      val coll = collectionToSaveIn()
      logger.trace(s"function=save id=${i.id} collection=${coll}")

      coll.foreach { c => c.save(dbo) }

      if (coll.isEmpty) {
        logger.warn(s"function=save - no collection found that can save the item with the id: ${i.id}")
      }
    }
  }

}

