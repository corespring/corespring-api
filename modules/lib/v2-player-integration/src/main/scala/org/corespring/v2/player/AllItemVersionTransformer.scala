package org.corespring.v2.player

import com.mongodb.casbah.Imports
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayItemTransformationCache }
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import play.api.Logger

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
      logger.debug(s"function=save id=${i.id}")
      val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(i))

      /**
       * Note: we have an auto boxing issue here - using VersionedIdImplicits.Reads to get around it.
       *
       * @return
       */

      def version(id: VersionedId[ObjectId]): Option[Int] = {
        import org.corespring.platform.core.models.versioning.VersionedIdImplicits._
        val json = play.api.libs.json.Json.toJson(id)
        (json \ "id").asOpt[Int]
      }

      def collectionToSaveIn = version(i.id).map { v =>
        val versionedCount = ItemServiceWired.dao.versionedCollection.count(MongoDBObject("_id._id" -> i.id.id, "_id.version" -> v))
        if (versionedCount == 1) {
          ItemServiceWired.dao.versionedCollection
        } else ItemServiceWired.dao.currentCollection
      }.getOrElse(ItemServiceWired.dao.currentCollection)
      logger.trace(s"function=save id=${i.id} collection=${collectionToSaveIn.name}")
      collectionToSaveIn.save(dbo)
    }
  }

}

