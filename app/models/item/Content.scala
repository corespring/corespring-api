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


  //TODO: This should be going through ItemService
  val collection = mongoCollection("content")

  def moveToArchive(contentId: VersionedId[ObjectId]): Either[InternalError, Unit] = {
    try {
      Content.collection.update(MongoDBObject("_id" -> contentId), MongoDBObject("$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString)),
        false, false, Content.collection.writeConcern)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to transfer content to archive", e))
    }
  }

  def isAuthorized(orgId: ObjectId, contentId: VersionedId[ObjectId], p: Permission): Boolean = {

    val dbo = itemService.findFieldsById(contentId)
    Logger.debug("found dbo: " + dbo)

    require(contentId.version.isDefined, "To Authorize access - the versioned id must be present")

    val elements = List("_id._id" -> contentId.id) ++ contentId.version.map("_id.version" -> _)
    val searchQuery: MongoDBObject = MongoDBObject(elements)

    collection.findOne(searchQuery, MongoDBObject(collectionId -> 1)).map {
      content: DBObject => content.get(collectionId) match {
        case s: String => isCollectionAuthorized(orgId, s, p)
        case _ => false
      }
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
