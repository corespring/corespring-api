package models

import org.bson.types.ObjectId
import se.radley.plugin.salat._
import mongoContext._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.Play.current
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import controllers.{LogType, InternalError}
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import dao.{SalatDAO, ModelCompanion, SalatInsertError, SalatDAOUpdateError}
import play.api.Play
import play.api.Play.current



/**
 * Case class representing an individual item session
 */
case class ItemSession (var itemId: ObjectId,
                         var start: Long = System.currentTimeMillis(),
                         var finish: Long = 0,
                         var responses: Seq[ItemResponse] = Seq(),
                         var id: ObjectId = new ObjectId()
                        )


/**
 * Companion object for ItemSession.
 * All operations specific to ItemSession are handled here
 *
 */
object ItemSession extends ModelCompanion[ItemSession,ObjectId] {
  val itemId = "itemId"
  val start = "start"
  val finish = "finish"
  val responses = "responses"

  val collection = mongoCollection("itemsessions")
  val dao = new SalatDAO[ItemSession, ObjectId](collection = collection) {}

  /**
   *
   * @param itemId - create the item session based on this contentId
   * @return - the newly created item session
   */
  def newItemSession(itemId:ObjectId, session:ItemSession):Either[InternalError, ItemSession] = {
      if(Play.isProd) session.id = new ObjectId()
      session.itemId = itemId
      session.start = System.currentTimeMillis()
      session.finish = 0
      try{
        ItemSession.insert(session,collection.writeConcern) match {
          case Some(_) => Right(session)
          case None => Left(InternalError("error inserting item session",LogType.printFatal))
        }
      }catch{
        case e:SalatInsertError => Left(InternalError("error inserting item session: "+e.getMessage, LogType.printFatal))
      }
  }

  def updateItemSession(session:ItemSession):Either[InternalError,Unit] = {
    val updatedbo = MongoDBObject.newBuilder
    if(session.finish != 0) updatedbo += "$set" -> MongoDBObject(finish -> session.finish)
    if (!session.responses.isEmpty) updatedbo += "$pushAll" -> MongoDBObject(responses -> session.responses.map(grater[ItemResponse].asDBObject(_)))
    try{
    ItemSession.update(MongoDBObject("_id" -> session.id, finish -> 0),
                      updatedbo.result(),
                      false,false,collection.writeConcern)
      Right(())
    }catch{
      case e:SalatDAOUpdateError => Left(InternalError("error updating item session: "+e.getMessage,LogType.printFatal))
    }
  }


  /**
   * Json Serializer
   */
  implicit object ItemSessionWrites extends Writes[ItemSession] {
    def writes(session: ItemSession) = {
          var seq:Seq[(String,JsValue)] = Seq(
            "id" -> JsString(session.id.toString),
            itemId -> JsString(session.itemId.toString),
            start -> JsNumber(session.start),
            responses -> Json.toJson(session.responses)
          )
          if(session.finish != 0) seq = seq :+ (finish -> JsNumber(session.finish))
          JsObject(seq)
    }
  }

  implicit object ItemSessionReads extends Reads[ItemSession] {
    def reads(json: JsValue):ItemSession = {
      ItemSession((json \ itemId).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        (json \ start).asOpt[Long].getOrElse(System.currentTimeMillis()),
        (json \ finish).asOpt[Long].getOrElse(0),
        (json \ responses).asOpt[Seq[ItemResponse]].getOrElse(Seq()),
        (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()))
    }
  }



}






