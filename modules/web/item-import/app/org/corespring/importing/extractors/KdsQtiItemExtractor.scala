package org.corespring.importing.extractors

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.resource.Resource
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.{ItemTransformer => KdsQtiItemTransformer, PathFlattener, ManifestReader, PassageTransformer}
import play.api.libs.json.{Json, JsValue}

import scala.io.Source
import scalaz._

abstract class KdsQtiItemExtractor(sources: Map[String, SourceWrapper]) extends ItemExtractor with PassageTransformer {

  import PathFlattener._

  val manifest = sources.find{ case(filename, _) => filename == ManifestReader.filename }
    .map { case(_, manifest) => ManifestReader.read(manifest, sources) }

  def ids = manifest.map(_.items.map(_.id)).getOrElse(Seq.empty)

  def metadata: Map[String, Validation[Error, Option[JsValue]]] =
    manifest.map(_.items.map(f =>
      f.id -> Success(Some(Json.obj("taskInfo" -> Json.obj("sourceId" -> "(.*).xml".r.replaceAllIn(f.filename, "$1")))))
    )).getOrElse(Seq.empty).toMap

  def filesFromManifest(id: String) = manifest.map(m => m.items.find(_.id == id)).flatten.map(item => item.resources)
    .getOrElse(Seq.empty).map(_.path.flattenPath)

  def files(id: String, itemId: VersionedId[ObjectId], itemJson: JsValue): Validation[Error, Option[Resource]] = {
    upload(itemId, sources.filter{ case (path, source) => filesFromManifest(id).contains(path.flattenPath) }) match {
      case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
      case Success(files) => Success(None)
      case Failure(error) => Failure(error)
    }
  }

  def itemJson: Map[String, Validation[Error, JsValue]] =
    manifest.map(_.items.map(f => sources.get(f.filename.flattenPath).map(s => {
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
