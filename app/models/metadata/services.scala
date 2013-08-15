package models.metadata

import models.{OrganizationService, MetadataSet}
import org.bson.types.ObjectId
import com.novus.salat.dao.{SalatDAO, DAO, ModelCompanion}
import se.radley.plugin.salat._
import com.mongodb.casbah.commons.MongoDBObject
import scala.Some
import com.mongodb.WriteResult
import models.item.Metadata
import org.corespring.platform.data.mongo.models.VersionedId
import models.item.service.{ItemServiceClient, ItemServiceImpl, ItemService}

trait MetadataService{

  def get(itemId:VersionedId[ObjectId], keys:Seq[String]): Seq[Metadata]
}

trait MetadataServiceImpl extends MetadataService{ self : ItemServiceClient =>

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Seq[Metadata] = {

    val maybeSeq : Option[Seq[Metadata]] = for{
      i <- itemService.findOneById(itemId)
      info <- i.taskInfo
    } yield info.extended.filter( e => keys.exists( _ == e.metadataKey ) )

    maybeSeq.getOrElse(Seq())
  }

}


trait MetadataSetService {
  def update(set: MetadataSet): Either[String, MetadataSet]

  def create(orgId: ObjectId, set: MetadataSet): Either[String, MetadataSet]

  def delete(orgId: ObjectId, setId: ObjectId): Option[String]

  def list(orgId: ObjectId): Seq[MetadataSet]

  def findByKey(key: String): Option[MetadataSet]

  def findOneById(id: ObjectId): Option[MetadataSet]
}


trait MetadataSetServiceImpl extends MetadataSetService {

  def orgService: OrganizationService

  private val dao = new ModelCompanion[MetadataSet, ObjectId] {
    import models.mongoContext.context
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