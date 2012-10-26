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
import dao.{SalatDAO, ModelCompanion, SalatInsertError, SalatDAOUpdateError}
import play.api.Play
import play.api.Play.current
import akka.actor.FSM.->
import scala.xml._
import qti.processors.FeedbackProcessor
import qti.models.QtiItem

case class FeedbackIdMapEntry(csFeedbackId: String, outcomeIdentifier: String, identifier: String)

/**
 * Case class representing an individual item session
 */
case class ItemSession(var itemId: ObjectId,
                       var start: Option[DateTime] = None,
                       var finish: Option[DateTime] = None,
                       var responses: Seq[ItemResponse] = Seq(),
                       var id: ObjectId = new ObjectId(),
                       var feedbackIdLookup: Seq[FeedbackIdMapEntry] = Seq(),
                       var sessionData: Option[SessionData] = None,
                       var settings: Option[ItemSessionSettings] = None
                        ) extends Identifiable

/**
 * Companion object for ItemSession.
 * All operations specific to ItemSession are handled here
 *
 */
object ItemSession extends ModelCompanion[ItemSession, ObjectId] {
  val itemId = "itemId"
  val start = "start"
  val finish = "finish"
  val responses = "responses"
  val sessionData = "sessionData"
  val settings = "settings"

  val collection = mongoCollection("itemsessions")
  val dao = new SalatDAO[ItemSession, ObjectId](collection = collection) {}

  /**
   *
   * @param itemId - create the item session based on this contentId
   * @return - the newly created item session
   */
  def newItemSession(itemId: ObjectId, session: ItemSession): Either[InternalError, ItemSession] = {
    if (Play.isProd) session.id = new ObjectId()
    session.itemId = itemId

    try {
      ItemSession.insert(session, collection.writeConcern) match {
        case Some(_) => Right(session)
        case None => Left(InternalError("error inserting item session", LogType.printFatal))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError("error inserting item session: " + e.getMessage, LogType.printFatal))
    }
  }

  /**
   * Start an item session, if the session is already started it returns an error.
   * @param session
   * @return either an InternalError or the updated ItemSession
   */
  def beginItemSession(session: ItemSession): Either[InternalError, ItemSession] = session.start match {

    case Some(_) => Left(InternalError("ItemSession already started: " + session.id, LogType.printFatal))
    case _ => {
      session.start = Some(new DateTime())
      ItemSession.save(session)
      Right(session)
    }
  }

  def getXmlWithFeedback(itemId: ObjectId, mapping: Seq[FeedbackIdMapEntry]): Either[InternalError, Elem] = {
    Item.getQti(itemId) match {
      case Right(qti) => Right(FeedbackProcessor.addFeedbackIds(XML.loadString(qti), mapping))
      case Left(e) => Left(e)
    }
  }

  def updateItemSession(session: ItemSession, xmlWithCsFeedbackIds: scala.xml.Elem): Either[InternalError, ItemSession] = {
    val updatedbo = MongoDBObject.newBuilder

    val dbo: BasicDBObject = new BasicDBObject()

    if (session.finish.isDefined) dbo.put(finish, session.finish.get)
    if (!session.responses.isEmpty) dbo.put(responses, session.responses.map(grater[ItemResponse].asDBObject(_)))

    if ( session.start.isEmpty ){
      session.settings match {
        case Some(s) => dbo.put(settings, grater[ItemSessionSettings].asDBObject(s))
        case _ => //do nothing
      }
    }

    updatedbo += ("$set" -> dbo)

    try {
      ItemSession.update(MongoDBObject("_id" -> session.id, finish -> MongoDBObject("$exists" -> false)),
        updatedbo.result(),
        false, false, collection.writeConcern)
      ItemSession.findOneById(session.id) match {
        case Some(session) => {
          //TODO - we need to flush the cache if session is finished
          session.sessionData = getSessionData(xmlWithCsFeedbackIds, session.responses)
          Right(session)
        }
        case None => Left(InternalError("could not find session that was just updated", LogType.printFatal))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("error updating item session: " + e.getMessage, LogType.printFatal))
    }
  }

  def getSessionData(xml: Elem, responses: Seq[ItemResponse]) = Some(SessionData(QtiItem(xml), responses))

  /**
   * Json Serializer
   */
  implicit object ItemSessionWrites extends Writes[ItemSession] {
    def writes(session: ItemSession) = {
      var seq: Seq[(String, JsValue)] = Seq(
        "id" -> JsString(session.id.toString),
        itemId -> JsString(session.itemId.toString),
        responses -> Json.toJson(session.responses)
      )
      if (session.sessionData.isDefined) {
        seq = seq :+ (sessionData -> Json.toJson(session.sessionData))
      }
      if (session.start.isDefined) {
        seq = seq :+ (start -> JsNumber(session.start.get.getMillis))
      }
      if (session.finish.isDefined) {
        seq = seq :+ (finish -> JsNumber(session.finish.get.getMillis))
      }
      if (session.settings.isDefined) {
        seq = seq :+ (settings -> Json.toJson(session.settings))
      }
      JsObject(seq)
    }
  }

  implicit object ItemSessionReads extends Reads[ItemSession] {
    def reads(json: JsValue): ItemSession = {

      /**
       * Note: if settings isn't there i
       */

      val settings = (json\"settings").asOpt[JsObject] match {
        case Some(jso) => jso.asOpt[ItemSessionSettings]
        case _ => None
      }

      ItemSession((json \ itemId).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        (json \ start).asOpt[Long].map(new DateTime(_)),
        (json \ finish).asOpt[Long].map(new DateTime(_)),
        (json \ responses).asOpt[Seq[ItemResponse]].getOrElse(Seq()),
        (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        settings = settings)
    }
  }


}






