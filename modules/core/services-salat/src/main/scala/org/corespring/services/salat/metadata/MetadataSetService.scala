package org.corespring.services.salat.metadata

import com.mongodb.WriteResult
import com.mongodb.casbah.commons.MongoDBObject
import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.models.metadata.MetadataSet
import org.corespring.services.salat.HasDao
import org.corespring.{ services => interface }

trait MetadataSetService extends interface.metadata.MetadataSetService with HasDao[MetadataSet, ObjectId] {

  private lazy val logger: Logger = Logger()

  def orgService: interface.OrganizationService

  override def update(set: MetadataSet): Either[String, MetadataSet] = {
    val result = dao.save(set)
    if (result.getLastError.ok) {
      Right(set)
    } else {
      Left(s"Error saving medatadata set: $set")
    }
  }

  override def findByKey(key: String): Option[MetadataSet] = dao.findOne(MongoDBObject("metadataKey" -> key))

  override def findOneById(id: ObjectId): Option[MetadataSet] = dao.findOneById(id)

  override def delete(orgId: ObjectId, setId: ObjectId): Option[String] = {
    val result: WriteResult = dao.removeById(setId)
    if (result.getLastError().ok()) {
      orgService.removeMetadataSet(orgId, setId).fold(e => Some(e.message), _ => None)
    } else {
      Some("Error removing set with id: " + setId)
    }
  }

  override def list(orgId: ObjectId): Seq[MetadataSet] = {

    //logger.debug(s"[list]: $orgId")

    orgService.findOneById(orgId).map {
      org =>
        org.metadataSets.map(ref => dao.findOneById(ref.metadataId)).flatten
    }.getOrElse(Seq())
  }

  override def create(orgId: ObjectId, set: MetadataSet): Either[String, MetadataSet] = {
    dao.insert(set).map {
      oid =>
        orgService.addMetadataSet(orgId, oid, false) match {
          case Left(e) => Left(e)
          case Right(ref) => Right(set.copy(id = oid))
        }
    }.getOrElse(Left("Error creating metadata set"))
  }
}
