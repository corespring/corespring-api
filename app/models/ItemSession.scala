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
                       var attempts: Int = 0,
                       var start: Option[DateTime] = None,
                       var finish: Option[DateTime] = None,
                       var responses: Seq[ItemResponse] = Seq(),
                       var id: ObjectId = new ObjectId(),
                       var feedbackIdLookup: Seq[FeedbackIdMapEntry] = Seq(),
                       var sessionData: Option[SessionData] = None,
                       var settings: ItemSessionSettings = new ItemSessionSettings()
                        ) extends Identifiable {

  def isStarted: Boolean = start.isDefined

  def isFinished: Boolean = finish.isDefined
}

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
  def newSession(itemId: ObjectId, session: ItemSession): Either[InternalError, ItemSession] = {
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
  def begin(session: ItemSession): Either[InternalError, ItemSession] = session.start match {

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


  def unfinishedSession(id: ObjectId): MongoDBObject = MongoDBObject("_id" -> id, finish -> MongoDBObject("$exists" -> false))


  /**
   * Update the itemSession model - this is not counted as the item being processed.
   * ItemSession can only be updated if its not started.
   * The only part of the session that is update-able is the settings.
   * @param update
   * @return
   */
  def update(update: ItemSession): Either[InternalError, ItemSession] =
    withDbSession(update) {
      dbSession =>

        val dbo: BasicDBObject = new BasicDBObject()

        if (!dbSession.isStarted) {
          dbo.put(settings, grater[ItemSessionSettings].asDBObject(update.settings))
        }

        if (dbo.size() > 0)
          updateFromDbo(update.id, MongoDBObject(("$set", dbo)))
        else
          Right(dbSession)

    }

  /**
   * find the db session and return it to fn
   * @param session
   * @param fn
   * @return
   */
  private def withDbSession(session: ItemSession)(fn: ((ItemSession) => Either[InternalError, ItemSession])) = {
    ItemSession.findOneById(session.id) match {
      case Some(dbSession) => fn(dbSession)
      case _ => Left(InternalError("can't find item session" + session.id.toString))
    }
  }

  private def updateFromDbo(id: ObjectId, dbo: DBObject, additionalProcessing: (ItemSession => Unit) = (s) => ()): Either[InternalError, ItemSession] = {
    try {
      ItemSession.update(unfinishedSession(id), dbo, false, false, collection.writeConcern)
      ItemSession.findOneById(id) match {
        case Some(session) => {
          additionalProcessing(session)
          Right(session)
        }
        case None => Left(InternalError("could not find session that was just updated", LogType.printFatal))
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("error updating item session: " + e.getMessage, LogType.printFatal))
    }
  }

  /**
   * Process the item session responses and return feedback.
   * If this iteration exceeds the number of attempts the finish it.
   * @param update
   * @param xmlWithCsFeedbackIds
   * @return
   */
  def process(update: ItemSession, xmlWithCsFeedbackIds: scala.xml.Elem): Either[InternalError, ItemSession] = withDbSession(update) {
    dbSession =>

      if (dbSession.isFinished) {
        Left(InternalError("The session is finished"))
      } else {

        val dbo: BasicDBObject = new BasicDBObject()

        if (!dbSession.isStarted) dbo.put(start, new DateTime())
        if (update.isFinished) dbo.put(finish, update.finish.get)
        if (!update.responses.isEmpty) dbo.put(responses, update.responses.map(grater[ItemResponse].asDBObject(_)))

        val dboUpdate = MongoDBObject(("$set", dbo), ("$inc", MongoDBObject(("attempts", 1))))

        updateFromDbo(update.id, dboUpdate, (u) => {
          finishSessionIfNeeded(u)
          val qtiItem = QtiItem(xmlWithCsFeedbackIds)
          u.sessionData = Some(SessionData(qtiItem, u.responses))
          u.responses.foreach(addOutcomeToResponse(qtiItem))
        })

      }
  }

  private def addOutcomeToResponse(qtiItem:QtiItem)(ir:ItemResponse) : Unit = {

    val id = ir.id
    qtiItem.responseDeclarations.find( _.identifier == id) match {
      case Some(rd) => {
        rd.isCorrect(ir.value)
      }
      case _ => //nothing
    }
  }

  private def finishSessionIfNeeded(session: ItemSession) {
    val max = session.settings.maxNoOfAttempts

    if (max != 0 && session.attempts >= session.settings.maxNoOfAttempts) {
      session.finish = Some(new DateTime())
      ItemSession.save(session)
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
        seq = seq :+ ("isStarted" -> JsBoolean(session.isStarted))
      }
      if (session.finish.isDefined) {
        seq = seq :+ (finish -> JsNumber(session.finish.get.getMillis))
        seq = seq :+ ("isFinished" -> JsBoolean(session.isFinished))
      }

      seq = seq :+ (settings -> Json.toJson(session.settings))

      JsObject(seq)
    }
  }

  implicit object ItemSessionReads extends Reads[ItemSession] {
    def reads(json: JsValue): ItemSession = {

      val settings = if ((json \ "settings").as[ItemSessionSettings] == null)
        new ItemSessionSettings()
      else
        (json \ "settings").as[ItemSessionSettings]

      ItemSession(
        itemId = (json \ itemId).asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        start = (json \ start).asOpt[Long].map(new DateTime(_)),
        finish = (json \ finish).asOpt[Long].map(new DateTime(_)),
        responses = (json \ responses).asOpt[Seq[ItemResponse]].getOrElse(Seq()),
        id = (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        settings = settings)
    }
  }


}






