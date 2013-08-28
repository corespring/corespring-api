package org.corespring.platform.core.services.metadata

import se.radley.plugin.salat.mongoCollection
import org.corespring.platform.core.services.organization.OrganizationService
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import se.radley.plugin.salat._
import com.novus.salat.dao._
import org.corespring.platform.core.models.metadata.{ Metadata, MetadataSet }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.services.item.ItemServiceClient
import play.api.Play.current
import org.corespring.platform.core.models.mongoContext._

trait MetadataService {

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Seq[Metadata]
}

trait MetadataServiceImpl extends MetadataService { self: ItemServiceClient =>

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Seq[Metadata] = {

    val maybeSeq: Option[Seq[Metadata]] = for {
      i <- itemService.findOneById(itemId)
      info <- i.taskInfo
      extendedMetadata <- toMetadataMap(info.extended)
    } yield extendedMetadata.filter(e => keys.exists(_ == e.key))

    maybeSeq.getOrElse(Seq())
  }

  def toMetadataMap(m: scala.collection.mutable.Map[String, BasicDBObject]): Option[Seq[Metadata]] = {

    def dboToMap(dbo: BasicDBObject): scala.collection.immutable.Map[String, String] = {
      import scala.collection.JavaConversions._
      val javaMap: java.util.Map[_, _] = dbo.toMap
      val scalaMap: scala.collection.mutable.Map[_, _] = mapAsScalaMap(javaMap)
      val stringMap: Seq[(String, String)] = scalaMap.toSeq.map {
        tuple =>
          val (key, value) = tuple
          println(s"key: $key, value: $value")

          (tuple._1.asInstanceOf[String], tuple._2.asInstanceOf[String])
      }
      scala.collection.immutable.Map(stringMap.toSeq: _*)
    }

    val out = m.toSeq.map(tuple => Metadata(tuple._1, dboToMap(tuple._2)))
    Some(out)
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
