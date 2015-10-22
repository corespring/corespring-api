package org.corespring.services.salat

import com.mongodb.casbah.Imports._
import com.novus.salat.Context
import com.novus.salat.dao.{ SalatDAO, SalatDAOUpdateError }
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollection, Organization }
import org.corespring.services.ContentCollectionUpdate
import org.corespring.services.errors._
import org.corespring.{ services => interface }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class ContentCollectionService(
  val dao: SalatDAO[ContentCollection, ObjectId],
  val context: Context,
  orgCollectionService: => interface.OrgCollectionService,
  val itemService: interface.item.ItemService,
  archiveConfig: ArchiveConfig) extends interface.ContentCollectionService with HasDao[ContentCollection, ObjectId] {

  object Keys {
    val isPublic = "isPublic"
    val sharedInCollections = "sharedInCollections"
  }

  override def create(name: String, org: Organization): Validation[PlatformServiceError, ContentCollection] = {
    val collection = ContentCollection(name = name, ownerOrgId = org.id)
    insertCollection(collection)
  }

  override def insertCollection(collection: ContentCollection): Validation[PlatformServiceError, ContentCollection] = {

    def addCollectionToDb() = Validation.fromTryCatch {
      dao.insert(collection).getOrElse {
        throw new RuntimeException(s"Insert failed for: $collection")
      }
    }.leftMap(t => CollectionInsertError(collection, Some(t)))

    //TODO: apply two-phase commit
    for {
      addedCollectionId <- addCollectionToDb()
      _ <- orgCollectionService.grantAccessToCollection(collection.ownerOrgId, addedCollectionId, Permission.Write)
    } yield collection.copy(id = addedCollectionId)
  }

  def isPublic(collectionId: ObjectId): Boolean = dao.findOneById(collectionId).exists(_.isPublic)

  override def delete(collId: ObjectId): Validation[PlatformServiceError, Unit] = {
    //todo: roll backs after detecting error in organization update

    val isEmptyCollection = itemService.countItemsInCollection(collId) match {
      case 0 => Success()
      case n => Failure(PlatformServiceError(s"Can't delete this collection it has $n item(s) in it."))
    }

    lazy val delete = for {
      _ <- Validation.fromTryCatch(dao.removeById(collId)).leftMap(t => PlatformServiceError(t.getMessage))
      _ <- orgCollectionService.removeAllAccessToCollection(collId)
      _ <- itemService.deleteFromSharedCollections(collId)
    } yield Unit

    for {
      _ <- isEmptyCollection
      _ <- delete
    } yield Unit
  }

  override def getPublicCollections: Seq[ContentCollection] = dao.find(MongoDBObject(Keys.isPublic -> true)).toSeq

  override def update(id: ObjectId, update: ContentCollectionUpdate): Validation[PlatformServiceError, ContentCollection] = {
    try {

      val query = MongoDBObject("_id" -> id)

      val updateDbo = {
        val fields = Seq(
          update.isPublic.map(p => "isPublic" -> p),
          update.name.map(n => "name" -> n)).flatten
        $set(fields: _*)
      }

      val result = dao.update(query, updateDbo, upsert = false, multi = false, dao.collection.writeConcern)

      if (result.getN == 1) {
        dao.findOneById(id).toSuccess(PlatformServiceError(s"Can't find collection with id: $id"))
      } else {
        Failure(PlatformServiceError(s"No update occurred for query: $id"))
      }
    } catch {
      case e: SalatDAOUpdateError => Failure(PlatformServiceError("failed to update collection", e))
    }
  }

  override def findOneById(id: ObjectId): Option[ContentCollection] = dao.findOneById(id)

  override def archiveCollectionId: ObjectId = archiveConfig.contentCollectionId

}
