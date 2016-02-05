package org.corespring.services.salat

import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors._
import org.corespring.models.appConfig.ArchiveConfig
import org.corespring.models.item.Passage
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Failure, Success, Validation}

class PassageService(
  dao: VersioningDao[Passage, VersionedId[ObjectId]],
  archiveConfig: ArchiveConfig,
  salatServicesExecutionContext: SalatServicesExecutionContext) extends interface.PassageService {

  implicit val ec: ExecutionContext = salatServicesExecutionContext.ctx

  lazy val logger = Logger(classOf[PassageService])

  override def get(passageId: VersionedId[ObjectId]): Future[Validation[PlatformServiceError, Option[Passage]]] =
    Future.successful({
      try {
        Success(dao.get(passageId))
      } catch {
        case e: Exception => {
          logger.error(e.getMessage)
          Failure(PassageReadError(passageId, Some(e)))
        }
      }
    })

  override def insert(passage: Passage): Future[Validation[PlatformServiceError, Passage]] =
    Future.successful({
      try {
        dao.insert(passage) match {
          case Some(passageId) => Success(passage.copy(id = passageId))
          case _ => Failure(PassageInsertError())
        }
      } catch {
        case e: Exception => {
          logger.error(e.getMessage)
          Failure(PassageInsertError(Some(e)))
        }
      }
    })

  override def save(passage: Passage): Future[Validation[PlatformServiceError, Passage]] = Future.successful({
    try {
      dao.save(passage, false) match {
        case Left(error) => Failure(PassageSaveError(passage.id))
        case Right(id) => Success(passage.copy(id = id))
      }
    } catch {
      case e: Exception => {
        logger.error(e.getMessage)
        Failure(PassageSaveError(passage.id, Some(e)))
      }
    }
  })

  override def delete(passageId: VersionedId[ObjectId]): Future[Validation[PlatformServiceError, Passage]] = {
    get(passageId).map{ _ match {
      case Success(Some(passage)) => {
        val update = MongoDBObject("$set" -> MongoDBObject(Passage.Keys.collectionId -> archiveConfig.contentCollectionId.toString))
        dao.update(passageId, update, createNewVersion = false)
        Success(passage.copy(collectionId = archiveConfig.contentCollectionId.toString))
      }
      case Success(None) => Failure(PassageReadError(passageId))
      case Failure(error) => Failure(error)
    }}

  }

}
