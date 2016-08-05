package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoCollection
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.{SessionServiceExecutionContext, SessionReportingUnsupported, SessionService}
import play.api.libs.json.JsValue

import scala.concurrent.Future

class MongoArchivedSessionService(primarySessionService: MongoSessionService, archivedCollection: MongoCollection,
                                  context: SessionServiceExecutionContext) extends SessionService with SessionReportingUnsupported {

  private val logger = Logger(classOf[MongoArchivedSessionService])
  private val archivedService = new MongoSessionService(archivedCollection, context)

  override def sessionCount(itemId: VersionedId[ObjectId]) =
    primarySessionService.sessionCount(itemId) + archivedService.sessionCount(itemId)

  override def loadMultiple(ids: Seq[String]): Future[Seq[(String, Option[JsValue])]] = {
    implicit val ec = context.ec
    primarySessionService.loadMultiple(ids).flatMap{ loaded =>
      archivedService.loadMultiple(loaded.filter(_._2.isEmpty).map(_._1)).map { loaded ++ _ }
    }
  }

  override def load(id: String): Option[JsValue] = primarySessionService.load(id) match {
    case Some(session) => Some(session)
    case _ => {
      val session = archivedService.load(id)
      session.map(session => logger.info(s"[load] session $id loaded from archive"))
      session
    }
  }

  override def save(id: String, data: JsValue, upsert: Boolean = false): Option[JsValue] =
    primarySessionService.save(id, data, upsert = false) match {
      case Some(session) => Some(session)
      case _ => {
        val savedSession = archivedService.save(id, data)
        savedSession.map(session => logger.info(s"[save] session $id saved to archive"))
        savedSession
      }
    }

  override def create(data: JsValue): Option[ObjectId] = primarySessionService.create(data)

}
