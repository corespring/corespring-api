package models.item

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatDAOUpdateError
import common.log.PackageLogging
import controllers.InternalError
import controllers.auth.Permission
import models.ContentCollection
import play.api.Play.current
import se.radley.plugin.salat._

trait Content {
  var id: ObjectId
  var contentType: String
  var collectionId: String
}

object Content extends PackageLogging {
  val collectionId: String = "collectionId"
  val contentType: String = "contentType"

  val collection = mongoCollection("content")

  def moveToArchive(contentId: ObjectId): Either[InternalError, Unit] = {
    try {
      Content.collection.update(MongoDBObject("_id" -> contentId), MongoDBObject("$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString)),
        false, false, Content.collection.writeConcern)
      Right(())
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("failed to transfer content to archive", e))
    }
  }

  def isAuthorized(orgId: ObjectId, contentId: ObjectId, p: Permission): Boolean = {
    val searchQuery: MongoDBObject = MongoDBObject("_id" -> contentId)

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

object ContentType {
  val item = "item"
  val assessment = "assessment"
  val materials = "materials"
}
