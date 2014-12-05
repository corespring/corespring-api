package org.corespring.importing

import java.io.{ByteArrayOutputStream, OutputStream}
import java.util.zip.{ZipOutputStream, ZipEntry}

import org.bson.types.ObjectId
import org.corespring.importing.extractors.{KdsQtiItemExtractor, CorespringItemExtractor}
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.item.{TaskInfo, PlayerDefinition}
import org.corespring.platform.core.models.item.resource.BaseFile
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.PathFlattener
import play.api.libs.json.{JsValue, Json}

import scala.io.Source
import scalaz.{Failure, Success}

/**
 * Mouthful of a class name. This class exports a provided QTI ZIP file to a ZIP file whose contents can be read by the
 * corespring-batch-importer (https://github.com/corespring/corespring-batch-importer)
 */
class ItemImporterExporter {

  import PathFlattener._

  def export(collection: ContentCollection, sources: Map[String, SourceWrapper]): Array[Byte] =
    createZip(toEntries(collection, sources))

  private def toEntries(collection: ContentCollection, sources: Map[String, SourceWrapper]): Map[String, Source] = {
    implicit val PlayerDefinitionFormat = PlayerDefinition.Format

    val extractor = new KdsQtiItemExtractor(sources) {
      def upload(itemId: VersionedId[ObjectId], files: Map[String, SourceWrapper]) = Success(Seq.empty[BaseFile])
    }

    def collectionName = s"${collection.name.toLowerCase.replaceAll(" ", "-")}_${collection.id.toString}"

    extractor.ids.map(id => {
      val itemJson = extractor.itemJson
      val meta = extractor.metadata
      val result = (itemJson.get(id).getOrElse(Failure(new Error("Missing item JSON"))),
        meta.get(id).getOrElse(Failure(new Error("Missing item metadata")))) match {
        case (Failure(error), _) => Failure(error)
        case (_, Failure(error)) => Failure(error)
        case (Success(itemJson), Success(md)) => {
          implicit val metadata = md
          Success(
            (
              PlayerDefinition(Seq.empty,
                (itemJson \ "xhtml").as[String],
                (itemJson \ "components"),
                (itemJson \ "summaryFeedback").asOpt[String].getOrElse(""),
                (itemJson \ "customScoring").asOpt[String]),
              taskInfo
            )
          )
        }
      }
      result match {
        case Success((definition, taskInfo)) => {
          val basePath = s"$collectionName/$id"
          Seq(s"$basePath/player-definition.json" -> Source.fromString(Json.prettyPrint(Json.toJson(definition))),
          s"$basePath/profile.json" -> Source.fromString(Json.prettyPrint(Json.obj("taskInfo" -> Json.toJson(taskInfo))))) ++
            extractor.filesFromManifest(id).map(filename => s"$basePath/data/${filename.flattenPath}" -> sources.get(filename))
              .filter { case (filename, maybeSource) => maybeSource.nonEmpty}
              .map { case (filename, someSource) => (filename, someSource.get.toSource) }
        }
        case _ => Seq.empty[(String, Source)]
      }
    }).flatten.toMap
  }

  private def taskInfo(implicit metadata: Option[JsValue]): Option[TaskInfo] = {
    implicit val taskInfoReads = TaskInfo.taskInfoReads
    metadata.map(md => (md \ "taskInfo").asOpt[TaskInfo]).flatten
  }

  private def createZip(files: Map[String, Source]) = {
    val bos = new ByteArrayOutputStream()
    val zipFile = new ZipOutputStream(bos)
    files.foreach{ case (filename, contents) => {
      zipFile.putNextEntry(new ZipEntry(filename))
      zipFile.write(contents.map(_.toByte).toArray)
    }}
    zipFile.close
    bos.toByteArray
  }

}
