package org.corespring.platform.core.models.itemSession

import com.mongodb.casbah.Imports._
import com.novus.salat._
import dao.{ SalatDAO, ModelCompanion, SalatInsertError, SalatDAOUpdateError }
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.json.JsonValidationException
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qti.models.responses.{ ResponseOutcome, Response }
import org.corespring.qti.models.{ FeedbackIdMapEntry, QtiItem }
import org.corespring.qti.processors.FeedbackProcessor
import org.joda.time.DateTime
import play.api.Play
import play.api.Play.current
import play.api.libs.json._
import scala.xml._
import se.radley.plugin.salat._
import org.corespring.platform.core.models.error.InternalError
import org.corespring.platform.core.services.item.{ ItemServiceImpl, ItemService }
import scalaz.{Failure, Success}

/**
 * Case class representing an individual item session
 */
case class ItemSession(var itemId: VersionedId[ObjectId],
  var attempts: Int = 0,
  var start: Option[DateTime] = None,
  var finish: Option[DateTime] = None,
  var responses: Seq[Response] = Seq(),
  var outcome: Option[SessionOutcome] = None,
  var id: ObjectId = new ObjectId(),
  var feedbackIdLookup: Seq[FeedbackIdMapEntry] = Seq(),
  var sessionData: Option[SessionData] = None,
  var settings: ItemSessionSettings = new ItemSessionSettings(),
  var dateModified: Option[DateTime] = None) {

  def isStarted: Boolean = start.isDefined

  def isFinished: Boolean = finish.isDefined
}

object ItemSession {

  object Keys {
    val itemId = "itemId"
    val itemVersion = "itemVersion"
    val start = "start"
    val finish = "finish"
    val responses = "responses"
    val sessionData = "sessionData"
    val settings = "settings"
    val dateModified = "dateModified"
  }
  implicit object Writes extends Writes[ItemSession] {

    def writes(session: ItemSession): JsValue = {

      import Keys._
      import VersionedIdImplicits.{ Writes => IdWrites }
      import play.api.libs.json.Json._

      val main: Seq[(String, JsValue)] = Seq(
        "id" -> JsString(session.id.toString),
        itemId -> Json.toJson(session.itemId)(IdWrites),
        responses -> toJson(session.responses),
        "settings" -> toJson(session.settings))

      val sessionDataSeq: Seq[(String, JsValue)] = Seq(
        session.sessionData.map(sd => ("sessionData" -> toJson(sd))),
        session.dateModified.map(dm => ("dateModified" -> JsNumber(dm.getMillis)))).flatten

      val startedSeq: Seq[(String, JsValue)] = session.start.map(s => {
        Seq(start -> JsNumber(s.getMillis), "isStarted" -> JsBoolean(true))
      }).getOrElse(Seq())

      val finishSeq: Seq[(String, JsValue)] = session.finish.map(f => {
        Seq(finish -> JsNumber(f.getMillis))
      }).getOrElse(Seq())

      val outcome: Seq[(String, JsValue)] = session.outcome.map(o => {
        Seq("outcome" -> Json.toJson(o))
      }).getOrElse(Seq())

      JsObject(main ++ sessionDataSeq ++ startedSeq ++ finishSeq ++ outcome)
    }
  }

  implicit object Reads extends Reads[ItemSession] {

    import Keys._

    override def reads(json: JsValue): JsResult[ItemSession] = {

      val settings = (json \ "settings").asOpt[ItemSessionSettings].getOrElse(ItemSessionSettings())

      import VersionedIdImplicits.{ Reads => IdReads }
      JsSuccess(ItemSession(
        itemId = (json \ itemId).asOpt[VersionedId[ObjectId]](IdReads).getOrElse(throw new JsonValidationException("You must have an item id")),
        start = (json \ start).asOpt[Long].map(new DateTime(_)),
        finish = (json \ finish).asOpt[Long].map(new DateTime(_)),
        responses = (json \ responses).asOpt[Seq[Response]].getOrElse(Seq()),
        id = (json \ "id").asOpt[String].map(new ObjectId(_)).getOrElse(new ObjectId()),
        settings = settings))
    }

  }

}

object PreviewItemSessionCompanion extends ItemSessionCompanion {
  override val debugMode = true;

  //Note: using a normal collection because in a capped collection a document's size may not grow beyond its original size.
  def collection = mongoCollection("itemsessionsPreview")
  def itemService: ItemService = ItemServiceImpl
}

object DefaultItemSession extends ItemSessionCompanion {
  override val debugMode = false

  def collection = mongoCollection("itemsessions")

  def itemService: ItemService = ItemServiceImpl
}
trait ItemSessionCompanion extends ModelCompanion[ItemSession, ObjectId] with PackageLogging {

  import ItemSession.Keys._
  import org.corespring.platform.core.models.mongoContext.context

  val debugMode:Boolean;

  def collection: MongoCollection

  def itemService: ItemService

  val dao = new SalatDAO[ItemSession, ObjectId](collection = collection) {}

  /**
   * @return - the newly created item session
   */
  def newSession(session: ItemSession): Either[InternalError, ItemSession] = {
    if (Play.isProd) session.id = new ObjectId()

    itemService.getQtiXml(session.itemId) match {
      case Some(xml) => {
        val (_, mapping) = FeedbackProcessor.addFeedbackIds(xml)
        session.feedbackIdLookup = mapping
      }
      case _ =>
    }

    try {
      insert(session, collection.writeConcern) match {
        case Some(_) => Right(session)
        case None => Left(InternalError("error inserting item session"))
      }
    } catch {
      case e: SalatInsertError => Left(InternalError("error inserting item session: " + e.getMessage))
    }
  }

  /**
   * Start an item session, if the session is already started it returns an error.
   * @param session
   * @return either an InternalError or the updated ItemSession
   */
  def begin(session: ItemSession): Either[InternalError, ItemSession] = session.start match {

    case Some(_) => Left(InternalError("ItemSession already started: " + session.id))
    case _ => {
      session.start = Some(new DateTime())
      save(session)
      Right(session)
    }
  }

  def getXmlWithFeedback(session: ItemSession): Either[InternalError, Elem] = {
    itemService.getQtiXml(session.itemId) match {
      case Some(qti) => Right(FeedbackProcessor.addFeedbackIds(qti, session.feedbackIdLookup))
      case _ => Left(InternalError("Can't find xml for itemId: " + session.itemId))
    }
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
    findOneById(session.id) match {
      case Some(dbSession) => fn(dbSession)
      case _ => Left(InternalError("can't find item session" + session.id.toString))
    }
  }

  private def updateFromDbo(id: ObjectId, dbo: DBObject, additionalProcessing: (ItemSession => Unit) = (s) => ()): Either[InternalError, ItemSession] = {
    try {
      logger.debug(this + ":: update into : " + this.collection.getFullName())
      //TODO: only update session if date modified is greater than date modified in db
      val wr = update(MongoDBObject("_id" -> id), dbo, false, false, collection.writeConcern)
      if(wr.getN() == 0){
        Left(InternalError("no open session could be updated"))
      } else {
        findOneById(id) match {
          case Some(session) => {
            additionalProcessing(session)
            Right(session)
          }
          case None => Left(InternalError("could not find session that was just updated"))
        }
      }
    } catch {
      case e: SalatDAOUpdateError => Left(InternalError("error updating item session: " + e.getMessage))
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
  def process(update: ItemSession, xmlWithCsFeedbackIds: scala.xml.Elem, isAttempt:Boolean = true)(implicit isInstructor:Boolean): Either[InternalError, ItemSession] = withDbSession(update) {
    dbSession =>

        val dbo: BasicDBObject = new BasicDBObject()

        if (!dbSession.isStarted) dbo.put(start, new DateTime())
        val attempts = if(isAttempt) dbSession.attempts + 1 else dbSession.attempts

        if (isAttempt && update.isFinished){
          dbo.put(finish, update.finish.get)
          dbo.put(dateModified, update.finish.get)
        } else{
          dbo.put(dateModified, new DateTime())
        }

        if (!update.responses.isEmpty) dbo.put(responses, update.responses.map(grater[Response].asDBObject(_)))
        dbo.put("attempts", attempts)
        val dboUpdate = MongoDBObject(("$set", dbo))

        updateFromDbo(update.id, dboUpdate, (u) => {
          if(isAttempt){
            val qtiItem = QtiItem(xmlWithCsFeedbackIds)
            val sessionOutcome = SessionOutcome.processSessionOutcome(u,qtiItem,debugMode)
            sessionOutcome match {
              case Success(so) => {
                u.outcome = Some(so)
                if (so.isComplete) {
                  if(!u.isFinished){
                    u.finish = Some(new DateTime())
                    u.dateModified = u.finish
                  }
                  save(u)
                }
                //TODO: We need to be careful with session data - you can't persist it
                u.sessionData = Some(SessionData(qtiItem, u)(isInstructor))
              }
              case Failure(error) => Left(error)
            }
          }
        })

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
    val maxScore = Score.getMaxScore(qti)
    val sessionScore = totalScore(session.responses, maxScore)
    (sessionScore, maxScore)
  }

  private def totalScore(responses: Seq[Response], scoreableResponses:Int): Double = {
    val sum = responses.foldRight[Double](0)((r,acc) => {
      acc + r.outcome.map(_.score.toDouble).getOrElse(0.toDouble)
    })
    val average = sum / scoreableResponses
    return average
  }

  /**
   * Get the item session and add any extra data to it if its finished.
   * @param id - the item session id
   * @return
   */
  def get(id: ObjectId)( implicit includeResponsesOverride:Boolean): Option[ItemSession] = {
    findOneById(id) match {
      case Some(session) => Some(addExtrasIfFinished(session, addSessionData, addResponses))
      case _ => None
    }
  }

  private def addExtrasIfFinished(
    session: ItemSession,
    fns: ((ItemSession, Elem) => Unit)*)( implicit includeResponsesOverride:Boolean): ItemSession =
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

  private def addSessionData(session: ItemSession, xml: Elem)( implicit includeResponsesOverride:Boolean) {
    session.sessionData = Some(SessionData(QtiItem(xml), session))
  }

  private def addResponses(session: ItemSession, xml: Elem)( implicit includeResponsesOverride:Boolean) {
    session.responses = Score.scoreResponses(session.responses, QtiItem(xml))
  }

}

