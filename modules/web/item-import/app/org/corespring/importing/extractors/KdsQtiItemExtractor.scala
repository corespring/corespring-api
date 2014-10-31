package org.corespring.importing.extractors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.{ItemTransformer => KdsQtiItemTransformer, ManifestReader, PassageTransformer}
import play.api.libs.json.{Json, JsValue}

import scala.io.Source
import scalaz._

abstract class KdsQtiItemExtractor(sources: Map[String, SourceWrapper]) extends ItemExtractor with PassageTransformer {

  val manifest = sources.find{ case(filename, _) => filename == ManifestReader.filename }
    .map { case(_, source) => ManifestReader.read(source, sources) }

  def ids = manifest.map(_.items.map(_.id)).getOrElse(Seq.empty)

  // TBD how to get metadata
  def metadata: Map[String, Validation[Error, Option[JsValue]]] = ids.map(_ -> Success(None)).toMap

  def files(id: String, itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]] = {
    val filesFromManifest = manifest.map(m => m.items.find(_.id == id)).flatten.map(item => item.resources)
      .getOrElse(Seq.empty).map(_.path)
    upload(itemId, sources.filter{ case (path, source) => filesFromManifest.contains(path) }) match {
      case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
      case Success(files) => Success(None)
      case Failure(error) => Failure(error)
    }
  }

  def itemJson: Map[String, Validation[Error, JsValue]] =
    manifest.map(_.items.map(f => sources.get(f.filename).map(s => {
      try {
        f.id -> Success(KdsQtiItemTransformer.transform(s.getLines.mkString, f, sources))
      } catch {
        case e: Exception => {
          e.printStackTrace()
          f.id -> Failure(new Error(s"There was an error translating ${f.id} into CoreSpring JSON"))
        }
      }
    })).flatten).getOrElse(Seq.empty).toMap

}
