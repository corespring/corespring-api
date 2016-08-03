package org.corespring.importing

import com.fasterxml.jackson.core.JsonParseException
import org.bson.types.ObjectId
import org.corespring.assets.AssetKeys
import org.corespring.importing.validation.{ ItemJsonValidator }
import org.corespring.models.item._
import org.corespring.models.item.resource.{ BaseFile, Resource }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import play.api.Logger
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration._
import scala.io.Source
import scalaz.{ Success, Failure, Validation }

case class ImportingExecutionContext(ctx: ExecutionContext)

class ItemFileConverter(
  uploader: Uploader,
  itemAssetKeys: AssetKeys[VersionedId[ObjectId]],
  itemService: ItemService,
  jsonFormatting: JsonFormatting,
  context: ImportingExecutionContext,
  itemValidator: ItemJsonValidator) {

  implicit val ec = context.ctx

  private val logger = Logger(classOf[ItemFileConverter])

  import jsonFormatting._

  object errors {
    val cannotCreateItem = "There was an error saving the item to the database"
    def fileMissing(filename: String) = s"Provided item source did not include $filename"
    def jsonParseError(filename: String) = s"$filename did not contain valid json"
    def metadataParseError(field: String) = s"There was an error parsing $field in $itemMetadataFilename"
  }

  case class ConversionException(error: Error) extends RuntimeException

  import errors._

  val itemJsonFilename = "item.json"
  val itemMetadataFilename = "metadata.json"

  /**
   * Takes a map of Sources, mapping their filename to the source data, and returns an Either of a CoreSpring Item
   * object or an Error.
   */
  def convert(collectionId: String)(implicit sources: Map[String, Source]): Validation[Error, Item] = {
    try {
      (loadJson(sources), metadata) match {
        case (Failure(error), _) => Failure(error)
        case (_, Failure(error)) => Failure(error)
        case (Success(json), Success(md)) => {
          implicit val metadata = md
          create(collectionId) match {
            case Some(id) => {
              val itemFiles: Option[Resource] = files(id, json) match {
                case Success(files) => files
                case Failure(error) => throw new ConversionException(error)
              }
              val supporting = supportingMaterials(id) match {
                case Success(supportingMaterials) => supportingMaterials
                case Failure(error) => throw new ConversionException(error)
              }
              val item = Item(
                id = id,
                collectionId = collectionId,
                contributorDetails = md.flatMap(m => contributorDetails((m \ "contributorDetails"))),
                data = itemFiles,
                lexile = extractString("lexile"),
                otherAlignments = otherAlignments,
                pValue = extractString("pValue"),
                playerDefinition =
                  Some(PlayerDefinition(
                    itemFiles.map(_.files).getOrElse(Seq.empty),
                    (json \ "xhtml").as[String],
                    (json \ "components"),
                    (json \ "summaryFeedback").asOpt[String].getOrElse(""),
                    None,
                    (json \ "config")
                    )),
                priorGradeLevels = extractStringSeq("priorGradeLevels"),
                priorUse = extractString("priorUse"),
                taskInfo = taskInfo,
                reviewsPassed = extractStringSeq("reviewsPassed"),
                standards = extractStringSeq("standards"),
                supportingMaterials = supporting,
                workflow = workflow)
              itemService.save(item, createNewVersion = false)
              Success(item)
            }
            case None => Failure(new Error(cannotCreateItem))
          }
        }
      }
    } catch {
      case ie: ConversionException => Failure(ie.error)
    }
  }

  import scalaz.Scalaz._

  private def loadJson(sources: Map[String, Source]): Validation[Error, JsValue] = {

    logger.trace(s"function=loadJson, sources=${sources.map(_._1)}")
    for {
      source <- sources.get(itemJsonFilename).toSuccess(new Error(s"Can't find file $itemJsonFilename"))
      itemJson <- Validation.fromTryCatch(Json.parse(source.mkString)).leftMap(_ => new Error("Can't parse json"))
      validated <- itemValidator.validate(itemJson).leftMap(e => new Error(e.mkString("\n")))
    } yield validated
  }

  private def metadata(implicit sources: Map[String, Source]): Validation[Error, Option[JsValue]] = {
    try {
      sources.get(itemMetadataFilename).map(item => Success(Some(Json.parse(item.mkString)))).getOrElse(Success(None))
    } catch {
      case json: JsonParseException => Failure(new Error(jsonParseError(itemMetadataFilename)))
    }
  }

  private def create(collectionId: String): Option[VersionedId[ObjectId]] = {
    val item = Item(
      collectionId = collectionId,
      playerDefinition = Some(PlayerDefinition.empty))
    itemService.insert(item)
  }

  private def files(itemId: VersionedId[ObjectId], itemJson: JsValue)(implicit sources: Map[String, Source]): Validation[Error, Option[Resource]] = {
    upload(itemId, sources.filter { case (filename, source) => (itemJson \ "files").asOpt[Seq[String]].getOrElse(Seq.empty).contains(filename) }) match {
      case Success(files) if files.nonEmpty => Success(Some(Resource(name = "data", files = files)))
      case Success(files) => Success(None)
      case Failure(error) => Failure(error)
    }
  }

  private def upload(itemId: VersionedId[ObjectId], files: Map[String, Source]): Validation[Error, Seq[BaseFile]] = {

    val uploads: Seq[Future[BaseFile]] = files.map {
      case (filename, source) => {
        val key = itemAssetKeys.file(itemId, filename)
        uploader.upload(filename, key, source)
      }
    }.toSeq

    Validation.fromTryCatch {
      val futures = Future.sequence(uploads)
      Await.result(futures, 5.minutes)
    }.leftMap(t => new Error(t.getMessage))
  }

  private def extractString(field: String)(implicit metadata: Option[JsValue]): Option[String] =
    metadata.map(metadata => (metadata \ field).asOpt[String]).flatten

  private def extractStringSeq(field: String)(implicit metadata: Option[JsValue]): Seq[String] =
    metadata.map(metadata => (metadata \ field).asOpt[Seq[String]]).flatten.getOrElse(Seq.empty)

  private def otherAlignments(implicit metadata: Option[JsValue]): Option[Alignments] = {
    metadata.map(md => (md \ "otherAlignments").asOpt[JsObject]).flatten.map(Json.fromJson[Alignments](_) match {
      case JsSuccess(value, _) => value
      case _ => throw new ConversionException(new Error(metadataParseError("otherAlignments")))
    })
  }

  private def contributorDetails(details: JsValue): Option[ContributorDetails] = for {
    cd <- details.asOpt[ContributorDetails]
  } yield {
    val copyright = (details \ "copyright").asOpt[Copyright](Json.reads[Copyright])
    cd.copy(copyright = copyright)
  }

  private def taskInfo(implicit metadata: Option[JsValue]): Option[TaskInfo] = {
    metadata.map(md => (md \ "taskInfo").asOpt[TaskInfo]).flatten
  }

  private def workflow(implicit metadata: Option[JsValue]): Option[Workflow] = {

    import Workflow.Keys

    metadata.map(md => (md \ "workflow").asOpt[Seq[String]] match {
      case Some(workflowStrings) => Some(Workflow(
        setup = workflowStrings.contains(Keys.setup),
        tagged = workflowStrings.contains(Keys.tagged),
        standardsAligned = workflowStrings.contains(Keys.standardsAligned),
        qaReview = workflowStrings.contains(Keys.qaReview)))
      case _ => None
    }).flatten
  }

  private def supportingMaterials(itemId: VersionedId[ObjectId])(implicit metadata: Option[JsValue], sources: Map[String, Source]): Validation[Error, Seq[Resource]] = {
    try {
      Success(metadata.map(md => (md \ "supportingMaterials").asOpt[Seq[JsObject]]).flatten.getOrElse(Seq.empty)
        .map(material => {
          val name = (material \ "name").asOpt[String].getOrElse("")
          val filenames = (material \ "files").asOpt[Seq[JsObject]].getOrElse(Seq.empty).map(f => (f \ "name").asOpt[String]).flatten
          upload(itemId, files = sources.filter { case (filename, source) => filenames.contains(filename) }) match {
            case Success(files) => Resource(name = name, files = files)
            case Failure(error) => throw new ConversionException(error)
          }
        }))
    } catch {
      case e: ConversionException => Failure(e.error)
    }

  }
}

