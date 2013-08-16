package org.corespring.platform.core.services.metadata

import com.mongodb.WriteResult
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat.dao.{SalatDAO, DAO, ModelCompanion}
import org.bson.types.ObjectId
import org.corespring.platform.core.models.metadata.MetadataSet
import se.radley.plugin.salat.mongoCollection
import org.corespring.platform.core.services.organization.OrganizationService

trait MetadataSetServiceImpl extends MetadataService {

  def orgService: OrganizationService

  private val dao = new ModelCompanion[MetadataSet, ObjectId] {

    import org.corespring.platform.core.models.mongoContext.context
    import play.api.Play.current
    val collection = mongoCollection("metadataSets")

    def dao: DAO[MetadataSet, ObjectId] = new SalatDAO[MetadataSet, ObjectId](collection = collection) {}
  }

  def findByKey(key: String): Option[MetadataSet] = dao.findOne(MongoDBObject("metadataKey" -> key))

  def update(set: MetadataSet): Either[String, MetadataSet] = {
    val result = dao.save(set)
    if (result.getLastError.ok) {
      Right(set)
    } else {
      Left("Error updating metadata set")
    }
  }

  def create(orgId: ObjectId, set: MetadataSet): Either[String, MetadataSet] = {
    dao.insert(set).map {
      oid =>
        orgService.addMetadataSet(orgId, oid, false) match {
          case Left(e) => Left(e)
          case Right(ref) => Right(set.copy(id = oid))
        }
    }.getOrElse(Left("Error creating metadata set"))
  }

  def delete(orgId: ObjectId, setId: ObjectId): Option[String] = {
    val result: WriteResult = dao.removeById(setId)
    if (result.getLastError().ok()) {
      orgService.removeMetadataSet(orgId, setId)
    } else {
      Some("Error removing set with id: " + setId)
    }
  }

  def list(orgId: ObjectId): Seq[MetadataSet] = {

    orgService.findOneById(orgId).map {
      org =>
        org.metadataSets.map(ref => dao.findOneById(ref.metadataId)).flatten
    }.getOrElse(Seq())
  }

  def findOneById(id: ObjectId): Option[MetadataSet] = dao.findOneById(id)
}
