package models.item

import com.mongodb.casbah.Imports._
import com.novus.salat.dao.SalatDAOUpdateError
import controllers.InternalError
import controllers.auth.Permission
import models.ContentCollection
import play.api.Play.current
import se.radley.plugin.salat._
import common.log.PackageLogging

trait Content{
  var id: ObjectId
  var contentType: String
  var collectionId: String
  var version:Option[Version]
}

object Content extends PackageLogging{
  val collectionId: String = "collectionId"
  val contentType: String = "contentType"
  val version = "version"

  val collection = mongoCollection("content")

  def moveToArchive(contentId:ObjectId):Either[InternalError, Unit] = {
    try{
      Content.collection.update(MongoDBObject("_id" -> contentId), MongoDBObject("$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString)),
        false, false, Content.collection.writeConcern)
      Right(())
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError("failed to transfer content to archive", e))
    }
  }
  def isAuthorized(orgId:ObjectId, contentId:ObjectId, p:Permission, current:Boolean = false):Boolean = {
    val searchQuery:MongoDBObject = if(current)
      MongoDBObject("_id" -> contentId,
        "$or" -> MongoDBList(MongoDBObject(Content.version -> MongoDBObject("$exists" -> false)),MongoDBObject(Content.version+"."+Version.current -> true)))
    else MongoDBObject("_id" -> contentId)
    Content.collection.findOne(searchQuery,MongoDBObject(Content.collectionId -> 1)) match {
      case Some(dbo) =>
        dbo.get(Content.collectionId) match {
        case collId:String =>
          isCollectionAuthorized(orgId,collId,p)
        case _ => Logger.error("content did not contain collection id"); false
      }
      case None => false
    }
  }
  def isCollectionAuthorized(orgId:ObjectId, collId:String, p:Permission):Boolean = {
    val ids = ContentCollection.getCollectionIds(orgId,p)
    ids.exists(_.toString == collId)
  }
}

object ContentType {
  val item = "item"
  val assessment = "assessment"
  val materials = "materials"
}
