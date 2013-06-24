package models.item

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatDAOUpdateError
import common.log.PackageLogging
import controllers.InternalError
import controllers.auth.Permission
import models.ContentCollection
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.Play.current
import se.radley.plugin.salat._
import models.item.service.{ItemServiceImpl, ItemService}

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
      //Content.collection.update(),
      //  false, false, Content.collection.writeConcern)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to transfer content to archive", e))
    }
  }

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Boolean = {

    require(contentId.version.isDefined, "To Authorize access - the versioned id must be present")
    //TODO: We should only find the item once - here we find it and return true/false which is wasteful.
    itemService.findOneById(contentId).map{ item =>
      isCollectionAuthorized(orgId, item.collectionId, p)
    }.getOrElse(false)
  }

  def isCollectionAuthorized(orgId: ObjectId, collId: String, p: Permission): Boolean = {
    val ids = ContentCollection.getCollectionIds(orgId, p)
    ids.exists(_.toString == collId)
  }
}

object Content extends ContentHelper(ItemServiceImpl)

object ContentType {
  val item = "item"
  val assessment = "assessment"
  val materials = "materials"
}
