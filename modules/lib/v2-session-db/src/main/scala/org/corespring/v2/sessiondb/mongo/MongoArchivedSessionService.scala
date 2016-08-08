package org.corespring.v2.sessiondb.mongo

import com.mongodb.casbah.MongoCollection
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.sessiondb.{SessionServiceExecutionContext, SessionReportingUnsupported, SessionService}
import play.api.libs.json.JsValue

import scala.concurrent.Future

class MongoArchivedSessionService(primarySessionService: MongoSessionService, archivedCollection: MongoCollection,
                                  context: SessionServiceExecutionContext) extends SessionService with SessionReportingUnsupported {

  val archivedService = new MongoSessionService(archivedCollection, context)

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
    case _ => archivedService.load(id)
  }

  override def save(id: String, data: JsValue, upsert: Boolean = false): Option[JsValue] =
    primarySessionService.save(id, data, upsert = false) match {
      case Some(session) => Some(session)
      case _ => archivedService.save(id, data)
    }

  override def create(data: JsValue): Option[ObjectId] = primarySessionService.create(data)

}
