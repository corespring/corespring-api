package models

import org.bson.types.ObjectId
import play.api.libs.json.JsValue
import se.radley.plugin.salat._
import play.api.Play.current
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.SalatDAOUpdateError
import controllers.{LogType, InternalError}

trait Content {
  var id: ObjectId;
  val contentType: String;
  var collectionId: ObjectId;

  def toJson(): JsValue;
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
}

object ContentType {
  val item = "item"
  val assessment = "assessment"

  def isContentType(s: String): Boolean = s == item || s == assessment
}
