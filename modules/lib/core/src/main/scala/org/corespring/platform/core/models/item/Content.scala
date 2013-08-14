package org.corespring.platform.core.models.item

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatDAOUpdateError
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.ContentCollection
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}


trait Content {
  var id: VersionedId[ObjectId]
  var contentType: String
  var collectionId: String
}

class ContentHelper(itemService:ItemService) extends PackageLogging {
  val collectionId: String = "collectionId"
  val contentType: String = "contentType"

  def moveToArchive(contentId: VersionedId[ObjectId]): Either[InternalError, Unit] = {
    try {
      val update = MongoDBObject( "$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString))
      itemService.saveUsingDbo(contentId, update, false)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to transfer content to archive", e))
    }
  }

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Boolean = {
    //TODO: We should only find the item once - here we find it and return true/false which is wasteful.
    itemService.findOneById(contentId).map{ item =>
      isCollectionAuthorized(orgId, item.collectionId, p)
    }.getOrElse{
      Logger.debug("isAuthorized: can't find item with id: " + contentId)
      false
    }
  }

  def isCollectionAuthorized(orgId: ObjectId, collectionId: String, p: Permission): Boolean = {
    val ids = ContentCollection.getCollectionIds(orgId, p)
    Logger.debug("isCollectionAuthorized: " + ids + " collection id: " + collectionId)
    ids.exists(_.toString == collectionId)
  }
}

object Content extends ContentHelper(ItemServiceImpl)

object ContentType {
  val item = "item"
  val assessment = "assessment"
  val materials = "materials"
}
