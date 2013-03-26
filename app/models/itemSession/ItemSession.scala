package models.itemSession

import se.radley.plugin.salat._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import controllers.{LogType, InternalError}
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.{SalatDAO, ModelCompanion, SalatInsertError, SalatDAOUpdateError}
import play.api.Play
import play.api.Play.current
import scala.xml._
import qti.processors.FeedbackProcessor
import qti.models.QtiItem
import models.mongoContext._
import models.item._
import models.item.resource._

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
                        ) {

  def isStarted: Boolean = start.isDefined

  def isFinished: Boolean = finish.isDefined
}

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
   * @param itemId - create the item session based on this contentId
   * @return - the newly created item session
   */
  def newSession(itemId: ObjectId, session: ItemSession): Either[InternalError, ItemSession] = {
    if (Play.isProd) session.id = new ObjectId()
    session.itemId = itemId

    getQtiXml(itemId) match {
      case Some(xml) => {
        val (_, mapping) = FeedbackProcessor.addFeedbackIds(xml)
        session.feedbackIdLookup = mapping
      }
      case _ =>
    }

    try {
      ItemSession.insert(session, collection.writeConcern) match {
        case Some(_) => Right(session)
        case None => Left(InternalError("error inserting item session", LogType.printFatal))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError("error inserting item session: " + e.getMessage, LogType.printFatal))
    }
  }

  private def getQtiXml(itemId: ObjectId): Option[Elem] = {
    Item.findOneById(itemId) match {
      case Some(item) => {
        val dataResource = item.data.get
        dataResource.files.find(_.name == Resource.QtiXml) match {
          case Some(qtiXml) => Some(scala.xml.XML.loadString(qtiXml.asInstanceOf[VirtualFile].content))
          case _ => None
        }
      }
      case _ => None
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

  def getXmlWithFeedback(session: ItemSession): Either[InternalError, Elem] = {
    Item.getQti(session.itemId) match {
      case Right(qti) => Right(FeedbackProcessor.addFeedbackIds(XML.loadString(qti), session.feedbackIdLookup))
      case Left(e) => Left(e)
    }
  }


  def unfinishedSession(id: ObjectId): MongoDBObject = MongoDBObject("_id" -> id, finish -> MongoDBObject("$exists" -> false))


  def findMultiple(oids: Seq[ObjectId]): Seq[ItemSession] = {
    val query = MongoDBObject("_id" -> MongoDBObject("$in" -> oids))
    ItemSession.find(query).toSeq//.map(addExtrasIfFinished(_, addResponses))
  }

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
   * find the db session and call fn with it passed in
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
   * TODO: process should only take one argument - ItemSession. The xml is always the xml linked via that session item.
   * Process the item session responses and return feedback.
   * If this iteration exceeds the number of attempts the finish it
   * Or if there are no incorrect responses finish it.
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
          val qtiItem = QtiItem(xmlWithCsFeedbackIds)
          u.responses = Score.scoreResponses(u.responses, qtiItem)
          finishSessionIfNeeded(u)
          //TODO: We need to be careful with session data - you can't persist it

          u.sessionData = Some(SessionData(qtiItem, u))
        })

      }
  }


  /**
   * Calculate the score total
   * @param session
   * @return a tuple (score, maxScore)
   */
  def getTotalScore(session: ItemSession): (Double, Double) = {
    require(session.isFinished, "The session isn't finished.")

    val xml = ItemSession.getXmlWithFeedback(session) match {
      case Left(e) => throw new RuntimeException("Error scoring - can't get xml")
      case Right(x) => x
    }

    val qti = QtiItem(xml)
    session.responses = Score.scoreResponses(session.responses, qti)
    session.sessionData = Some(SessionData(qti, session))

    val correctResponsesScored = Score.scoreResponses(session.sessionData.get.correctResponses, qti)

    def processScore(responses: Seq[ItemResponse], processFn: Float => Double) = {
      responses.foldLeft(0.0) {
        (acc, r) =>
          val current = r.outcome match {
            case None => 0
            case Some(v) => processFn(v.score)
          }
          acc + current
      }
    }

    def score = processScore(session.responses, (s) => s)
    def maxScore = processScore(correctResponsesScored, (s) => 1.0)
    (score, maxScore)
  }

  /**
   * Get the item session and add any extra data to it if its finished.
   * @param id - the item session id
   * @return
   */
  def get(id: ObjectId): Option[ItemSession] = {
    ItemSession.findOneById(id) match {
      case Some(session) => Some(addExtrasIfFinished(session, addSessionData, addResponses))
      case _ => None
    }
  }

  private def addExtrasIfFinished(
                         session: ItemSession,
                         fns: ((ItemSession, Elem) => Unit)*): ItemSession =
    if (session.isFinished) {
      getXmlWithFeedback(session) match {
        case Right(xml) => {
          fns.foreach(_(session, xml))
          session
        }
        case Left(e) => session
      }
    } else {
      session
    }

  private def addSessionData(session: ItemSession, xml: Elem) {
    session.sessionData = Some(SessionData(QtiItem(xml), session))
  }

  private def addResponses(session: ItemSession, xml: Elem) {
    session.responses = Score.scoreResponses(session.responses, QtiItem(xml))
  }

  private def finishSessionIfNeeded(session: ItemSession) {

    def finishIfMaxAttemptsExceeded() {
      val max = session.settings.maxNoOfAttempts
      if (max != 0 && session.attempts >= session.settings.maxNoOfAttempts) {
        session.finish = Some(new DateTime())
        ItemSession.save(session)
      }
    }

    def finishIfThereAreNoIncorrectResponses() {
      val incorrectResponses = session.responses.filter(_.outcome match {
        case None => false
        case Some(o) => if (o.score <= 0) true else false
      })

      if (incorrectResponses.length == 0) {
        session.finish = Some(new DateTime())
        ItemSession.save(session)
      }
    }

    finishIfMaxAttemptsExceeded()
    finishIfThereAreNoIncorrectResponses()
  }


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






