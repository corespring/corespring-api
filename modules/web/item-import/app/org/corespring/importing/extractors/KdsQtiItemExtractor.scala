package org.corespring.importing.extractors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.{Resource, BaseFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.ManifestReader
import org.corespring.qtiToV2.kds.{ItemTransformer => KdsQtiItemTransformer }
import play.api.libs.json.{Json, JsValue}

import scala.io.Source
import scalaz._

abstract class KdsQtiItemExtractor(sources: Map[String, Source]) extends ItemExtractor {

  val manifest = sources.find{ case(filename, _) => filename == ManifestReader.filename }
    .map { case(_, source) => ManifestReader.read(source) }

  def ids = manifest.map(_.items.map(_.id)).getOrElse(Seq.empty)

  // TBD how to get metadata
  def metadata: Map[String, Validation[Error, Option[JsValue]]] = ids.map(_ -> Success(None)).toMap

  def files(itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]] =
    upload(itemId, sources.filter{ case (filename, source) => (itemJson \ "files").asOpt[Seq[String]]
      .getOrElse(Seq.empty).contains(filename) }) match {
        case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
        case Success(files) => Success(None)
        case Failure(error) => Failure(error)
      }

  def itemJson: Map[String, Validation[Error, JsValue]] =
    manifest.map(_.items.map(f => sources.get(f.filename).map(s => {
      try {
        f.id -> Success(KdsQtiItemTransformer.transform(s.getLines.mkString))
      } catch {
        case e: Exception => {
          e.printStackTrace()
          f.id -> Failure(new Error(s"There was an error translating ${f.id} into CoreSpring JSON"))
        }
      }
    })).flatten).getOrElse(Seq.empty).toMap
}
