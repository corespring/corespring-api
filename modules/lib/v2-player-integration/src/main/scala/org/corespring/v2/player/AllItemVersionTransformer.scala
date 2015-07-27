package org.corespring.v2.player

import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON
import com.novus.salat.Context
import org.bson.types.ObjectId
import org.corespring.models.item.{ FieldValue, Item }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.bootstrap.Services
import org.corespring.services.{ StandardService, SubjectService }
import org.corespring.services.item.{ BaseFindAndSaveService }
import org.corespring.services.errors.{ GeneralError, PlatformServiceError }
import play.api.{ Play, Configuration, Logger }

/**
 * Note: Whilst we support v1 and v2 players, we need to allow the item transformer to save 'versioned' items (aka not the most recent item).
 * This is not possible using the SalatVersioningDao as it has logic in place that prevents that happening.
 * @see SalatVersioningDao
 */
class AllItemVersionTransformer(
  services: Services,
  currentCollection: MongoCollection,
  versionedCollection: MongoCollection,
  val rootOrgId: ObjectId,
  implicit val context: Context) extends ItemTransformer {

  override lazy val logger = Logger("org.corespring.v2.player.AllItemsVersionTransformer")

  override def configuration: Configuration = Play.current.configuration

  override def findCollection(id: ObjectId) = services.contentCollection.findOneById(id)

  def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]] = new BaseFindAndSaveService[Item, VersionedId[ObjectId]] {

    override def findOneById(id: VersionedId[ObjectId]): Option[Item] = services.item.findOneById(id)

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

  override def fieldValue: FieldValue = services.fieldValue.get.get

  override def standardService: StandardService = services.standard

  override def subjectService: SubjectService = services.subject
}
