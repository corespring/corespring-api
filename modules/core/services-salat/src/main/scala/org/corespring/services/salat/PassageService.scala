package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.item.Passage
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.bootstrap.SalatServicesExecutionContext
import org.corespring.{ services => interface }

import scala.concurrent.{ExecutionContext, Future}

class PassageService(
  dao: VersioningDao[Passage, VersionedId[ObjectId]],
  salatServicesExecutionContext: SalatServicesExecutionContext) extends interface.PassageService {

  implicit val ec: ExecutionContext = salatServicesExecutionContext.ctx

  override def get(id: VersionedId[ObjectId]): Future[Option[Passage]] = Future.successful(dao.get(id))

  override def insert(passage: Passage): Future[Option[VersionedId[ObjectId]]] = Future.successful(dao.insert(passage))
}
