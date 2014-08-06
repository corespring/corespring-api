package org.corespring.v2.player

import com.mongodb.casbah.Imports
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayItemTransformationCache }
import org.corespring.platform.core.services.BaseFindAndSaveService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.v2.log.V2LoggerFactory

/**
 * Note: Whilst we support v1 and v2 players, we need to allow the item transformer to save 'versioned' items (aka not the most recent item).
 * This is not possible using the SalatVersioningDao as it has logic in place that prevents that happening.
 * @see SalatVersioningDao
 */
class AllItemVersionTransformer extends ItemTransformer {

  lazy val logger = V2LoggerFactory.getLogger("AllItemsVersionTransformer")
  def cache: ItemTransformationCache = PlayItemTransformationCache

  def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]] = new BaseFindAndSaveService[Item, VersionedId[ObjectId]] {

    override def findOneById(id: VersionedId[Imports.ObjectId]): Option[Item] = ItemServiceWired.findOneById(id)

    override def save(i: Item, createNewVersion: Boolean): Unit = {
      import com.mongodb.casbah.Imports._
      import com.novus.salat._
      import org.corespring.platform.core.models.mongoContext.context
      logger.debug(s"[itemTransformer.save] - saving versioned content directly")
      val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(i))
      ItemServiceWired.dao.currentCollection.save(dbo)
    }
  }

}
