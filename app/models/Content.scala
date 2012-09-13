package models

import org.bson.types.ObjectId
import play.api.libs.json.JsValue
import se.radley.plugin.salat._
import play.api.Play.current
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatDAOUpdateError
import controllers.{Log, LogType, InternalError}
import controllers.auth.Permission
import play.api.Logger

trait Content {
  var id: ObjectId;
  var contentType: String;
  var collectionId: String;

}

object Content {
  val collectionId: String = "collectionId"
  val contentType: String = "contentType"

  val collection = mongoCollection("content")

  def moveToArchive(contentId:ObjectId):Either[InternalError, Unit] = {
    try{
      Content.collection.update(MongoDBObject("_id" -> contentId), MongoDBObject("$set" -> MongoDBObject(Content.collectionId -> ContentCollection.archiveCollId.toString)),
        false, false, Content.collection.writeConcern)
      Right(())
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError(e.getMessage,LogType.printFatal,clientOutput = Some("failed to transfer content to archive")))
    }
  }
  def isAuthorized(orgId:ObjectId, contentId:ObjectId, p:Permission):Boolean = {
    Content.collection.findOneByID(contentId,MongoDBObject(Content.collectionId -> 1)) match {
      case Some(dbo) => dbo.get(Content.collectionId) match {
        case collId:String => isCollectionAuthorized(orgId,collId,p)
        case _ => Log.f("content did not contain collection id"); false
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
