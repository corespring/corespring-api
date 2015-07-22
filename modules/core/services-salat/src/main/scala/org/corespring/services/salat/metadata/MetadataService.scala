package org.corespring.services.salat.metadata

import com.mongodb.casbah.Imports._
import org.corespring.models.metadata.Metadata
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services

trait MetadataService extends services.metadata.MetadataService {

  def itemService: services.item.ItemService

  def get(itemId: VersionedId[ObjectId], keys: Seq[String]): Seq[Metadata] = {

    val maybeSeq: Option[Seq[Metadata]] = for {
      i <- itemService.findOneById(itemId)
      info <- i.taskInfo
      extendedMetadata <- toMetadataMap(info.extended)
    } yield extendedMetadata.filter(e => keys.exists(_ == e.key))

    maybeSeq.getOrElse(Seq())
  }

  def toMetadataMap(m: Map[String, DBObject]): Option[Seq[Metadata]] = {

    def dboToMap(dbo: DBObject): Map[String, String] = {
      import scala.collection.JavaConversions._
      val javaMap: java.util.Map[_, _] = dbo.toMap
      val scalaMap: scala.collection.mutable.Map[_, _] = mapAsScalaMap(javaMap)
      val stringMap: Seq[(String, String)] = scalaMap.toSeq.map {
        tuple =>
          val (key, value) = tuple
          (key.asInstanceOf[String], value.asInstanceOf[String])
      }
      scala.collection.immutable.Map(stringMap.toSeq: _*)
    }

    val out = m.toSeq.map(tuple => Metadata(tuple._1, dboToMap(tuple._2)))
    Some(out)
  }

}