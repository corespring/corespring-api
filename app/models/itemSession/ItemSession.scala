package models.itemSession

import se.radley.plugin.salat._
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import controllers.{Utils, LogType, InternalError}
import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.{SalatDAO, ModelCompanion, SalatInsertError, SalatDAOUpdateError}
import play.api.{Logger, Play}
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
                       var settings: ItemSessionSettings = new ItemSessionSettings(),
                       var dateModified: Option[DateTime] = None
                        ) {

  def isStarted: Boolean = start.isDefined

  def isFinished: Boolean = finish.isDefined
}

object ItemSession {

  object Keys {
    val itemId = "itemId"
    val start = "start"
    val finish = "finish"
    val responses = "responses"
    val sessionData = "sessionData"
    val settings = "settings"
    val dateModified = "dateModified"
  }

  implicit object Writes extends Writes[ItemSession] {


    def writes(session: ItemSession): JsValue = {

      val main: Seq[(String, JsValue)] = Seq(
        "id" -> JsString(session.id.toString),
        Keys.itemId -> JsString(session.itemId.toString),
        Keys.responses -> Json.toJson(session.responses),
        "settings" -> Json.toJson(session.settings)
      )

      val sessionDataSeq: Seq[(String, JsValue)] = Seq(
        session.sessionData.map(sd => ("sessionData" -> Json.toJson(sd))),
        session.dateModified.map(dm => ("dateModified" -> JsNumber(dm.getMillis)))
      ).flatten

      val startedSeq: Seq[(String, JsValue)] = session.start.map(s => {
        Seq(Keys.start -> JsNumber(s.getMillis), "isStarted" -> JsBoolean(true))
      }).getOrElse(Seq())

      val finishSeq: Seq[(String, JsValue)] = session.finish.map(f => {
        Seq(Keys.finish -> JsNumber(f.getMillis), "isFinished" -> JsBoolean(true))
      }).getOrElse(Seq())

      JsObject(main ++ sessionDataSeq ++ startedSeq ++ finishSeq)
    }
  }

  implicit object Reads extends Reads[ItemSession] {

    import Keys._

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

object PreviewItemSessionCompanion extends ItemSessionCompanion {
  def collection = mongoCollection("itemsessionsPreview")
}

object DefaultItemSession extends ItemSessionCompanion {
  def collection = mongoCollection("itemsessions")
}

trait ItemSessionCompanion extends ModelCompanion[ItemSession, ObjectId] {

  import ItemSession.Keys._

  def collection: MongoCollection

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
      insert(session, collection.writeConcern) match {
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
      save(session)
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
    find(query).toSeq //.map(addExtrasIfFinished(_, addResponses))
  }

  def findItemSessions(id: ObjectId): Seq[ItemSession] = Utils.toSeq(find(MongoDBObject(itemId -> id)))

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
    findOneById(session.id) match {
      case Some(dbSession) => fn(dbSession)
      case _ => Left(InternalError("can't find item session" + session.id.toString))
    }
  }

  private def updateFromDbo(id: ObjectId, dbo: DBObject, additionalProcessing: (ItemSession => Unit) = (s) => ()): Either[InternalError, ItemSession] = {
    try {
      Logger.debug(this + ":: update into : " + this.collection.getFullName())
      update(unfinishedSession(id), dbo, false, false, collection.writeConcern)
      findOneById(id) match {
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


  type Score = (Double, Double)

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

        dbo.put(dateModified, if (update.isFinished) update.finish.get else new DateTime())

        val dboUpdate = MongoDBObject(("$set", dbo), ("$inc", MongoDBObject(("attempts", 1))))

        def isMaxAttemptsExceeded(session: ItemSession): Boolean = {
          val max = session.settings.maxNoOfAttempts
          max != 0 && session.attempts >= max
        }

        def isTopScore(score: Score) = (score._1 / score._2) == 1

        updateFromDbo(update.id, dboUpdate, (u) => {
          val qtiItem = QtiItem(xmlWithCsFeedbackIds)
          u.responses = Score.scoreResponses(u.responses, qtiItem)
          val score: Score = (correctResponseCount(u.responses), Score.getMaxScore(qtiItem))

          if (isMaxAttemptsExceeded(u) || isTopScore(score)) {
            u.finish = Some(new DateTime())
            u.dateModified = u.finish
            save(u)
          }

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

    val xml = getXmlWithFeedback(session) match {
      case Left(e) => throw new RuntimeException("Error scoring - can't get xml")
      case Right(x) => x
    }

    val qti = QtiItem(xml)
    session.responses = Score.scoreResponses(session.responses, qti)

    val sessionScore = correctResponseCount(session.responses)
    (sessionScore, Score.getMaxScore(qti))
  }

  def correctResponseCount(responses: Seq[ItemResponse]): Double = {
    val outcomes: Seq[ItemResponseOutcome] = responses.map(_.outcome).flatten
    val outcomesCorrect = outcomes.map(_.isCorrect)
    outcomesCorrect.filter(_ == true).length
  }

  /**
   * Get the item session and add any extra data to it if its finished.
   * @param id - the item session id
   * @return
   */
  def get(id: ObjectId): Option[ItemSession] = {
    findOneById(id) match {
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


}






