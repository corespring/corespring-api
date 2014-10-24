package org.corespring.importing.extractors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.{Resource, BaseFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.ManifestReader
import org.corespring.qtiToV2.kds.{ItemTransformer => KdsQtiItemTransformer }
import play.api.libs.json.JsValue

import scala.io.Source
import scalaz._

abstract class KdsQtiItemExtractor(sources: Map[String, Source]) extends ItemExtractor {

  val manifest = sources.find{ case(filename, _) => filename == ManifestReader.filename }
    .map { case(_, source) => ManifestReader.read(source) }

  def metadata: Validation[Error, Option[JsValue]] = Success(None) // TBD how to get metadata

  // TODO: Support multiple items
  def files(itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]] =
    upload(itemId, sources.filter{ case (filename, source) => manifest.map(_.items.headOption)
      .flatten.map(_.resources).getOrElse(Seq.empty).contains(filename) }) match {
        case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
        case Success(files) => Success(None)
        case Failure(error) => Failure(error)
      }

  def itemJson: Validation[Error, JsValue] =
    manifest.map(_.items.headOption).flatten.map(f => sources.get(f.filename)).flatten
      .map(s => KdsQtiItemTransformer.transform(s.getLines.mkString)) match {
        case Some(json) => Success(json)
        case _ => Failure(new Error("something went wrong"))
      }
}
