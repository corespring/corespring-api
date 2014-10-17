package org.corespring.importing

import java.io.ByteArrayInputStream

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.transfer.TransferManager
import com.fasterxml.jackson.core.JsonParseException
import org.bson.types.ObjectId
import org.corespring.json.validation.JsonValidator
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.models.item.resource.{Resource, StoredFile, BaseFile}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.libs.json._

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait ItemFileConverter {

  def bucket: String
  def uploader: Uploader
  def auth: ItemAuth[OrgAndOpts]

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

  val dummyReturn = Right(PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None))

  /**
   * Takes a map of Sources, mapping their filename to the source data, and returns an Either of a CoreSpring Item
   * object or an Error.
   */
  def convert(collectionId: String, identity: OrgAndOpts)(implicit sources: Map[String, Source]): Either[Error, Item] = {
    try {
      (itemJson, metadata) match {
        case (Left(error), _) => Left(error)
        case (_, Left(error)) => Left(error)
        case (Right(itemJson), Right(md)) => {
          implicit val metadata = md
          create(collectionId, identity) match {
            case Some(id) => {
              val itemFiles: Option[Resource] = files(id, itemJson) match {
                case Right(files) => files
                case Left(error) => throw new ConversionException(error)
              }
              val supporting = supportingMaterials(id) match {
                case Right(supportingMaterials) => supportingMaterials
                case Left(error) => throw new ConversionException(error)
              }
              val item = Item(
                id = id,
                collectionId = Some(collectionId),
                contributorDetails = contributorDetails,
                data = itemFiles,
                lexile = extractString("lexile"),
                otherAlignments = otherAlignments,
                pValue = extractString("pValue"),
                playerDefinition =
                  Some(PlayerDefinition(itemFiles.map(_.files).getOrElse(Seq.empty),
                    (itemJson \ "xhtml").as[String], (itemJson \ "components"),
                    (itemJson \ "summaryFeedback").asOpt[String].getOrElse(""), None)),
                priorGradeLevels = extractStringSeq("priorGradeLevels"),
                priorUse = extractString("priorUse"),
                taskInfo = taskInfo,
                reviewsPassed = extractStringSeq("reviewsPassed"),
                standards = extractStringSeq("standards"),
                supportingMaterials = supporting,
                workflow = workflow
              )
              auth.save(item, createNewVersion = false)(identity)
              Right(item)
            }
            case None => Left(new Error(cannotCreateItem))
          }
        }
      }
    } catch {
      case ie: ConversionException => Left(ie.error)
    }
  }

  private def itemJson(implicit sources: Map[String, Source]): Either[Error, JsValue] = {
    val filename = itemJsonFilename
    try {
      val itemJson = sources.get(filename).map(item => Json.parse(item.mkString))
        .getOrElse(throw new Exception(errors.fileMissing(filename)))
      JsonValidator.validateItem(itemJson) match {
        case Left(errorMessages) => Left(new Error(errorMessages.mkString("\n")))
        case Right(itemJson) => Right(itemJson)
      }
    } catch {
      case json: JsonParseException => Left(new Error(jsonParseError(filename)))
      case e: Exception => Left(new Error(e.getMessage))
    }
  }

  private def metadata(implicit sources: Map[String, Source]): Either[Error, Option[JsValue]] = {
    try {
      sources.get(itemMetadataFilename).map(item => Right(Some(Json.parse(item.mkString)))).getOrElse(Right(None))
    } catch {
      case json: JsonParseException => Left(new Error(jsonParseError(itemMetadataFilename)))
    }
  }

  private def create(collectionId: String, identity: OrgAndOpts): Option[VersionedId[ObjectId]] = {
    val item = Item(
      collectionId = Some(collectionId),
      playerDefinition = Some(PlayerDefinition.empty))
    auth.insert(item)(identity)
  }

  private def files(itemId: VersionedId[ObjectId], itemJson: JsValue)(implicit sources: Map[String, Source]): Either[Error, Option[Resource]] = {
    upload(itemId, sources.filter{case (filename, source) => (itemJson \ "files").asOpt[Seq[String]].getOrElse(Seq.empty).contains(filename) }) match {
      case Right(files) if files.nonEmpty => Right(Some(Resource(name = "data", files = files)))
      case Right(files) => Right(None)
      case Left(error) => Left(error)
    }
  }

  private def upload(itemId: VersionedId[ObjectId], files: Map[String, Source]): Either[Error, Seq[BaseFile]] = {
    val futureFiles =
      Future.sequence(files.map{ case(filename, source) => uploader.upload(filename, s"$itemId/data/$filename", source) }.toSeq)
    try {
      Right(Await.result(futureFiles, Duration.Inf))
    } catch {
      case e: Exception => Left(new Error(e.getMessage))
    }
  }

  private def extractString(field: String)(implicit metadata: Option[JsValue]): Option[String] =
    metadata.map(metadata => (metadata \ field).asOpt[String]).flatten

  private def extractStringSeq(field: String)(implicit metadata: Option[JsValue]): Seq[String] =
    metadata.map(metadata => (metadata \ field).asOpt[Seq[String]]).flatten.getOrElse(Seq.empty)

  private def otherAlignments(implicit metadata: Option[JsValue]): Option[Alignments] = {
    implicit val alignmentsReads = Alignments.Reads
    metadata.map(md => (md \ "otherAlignments").asOpt[JsObject]).flatten.map(Json.fromJson[Alignments](_) match {
      case JsSuccess(value, _) => value
      case _ => throw new ConversionException(new Error(metadataParseError("otherAlignments")))
    })
  }

  private def contributorDetails(implicit metadata: Option[JsValue]): Option[ContributorDetails] = {
    implicit val contributorDetailsReads = ContributorDetails.Reads
    implicit val Formats = Json.format[Copyright]
    metadata.map(md => (md \ "contributorDetails").asOpt[JsObject]).flatten.map(js => Json.fromJson[ContributorDetails](js) match {
      case JsSuccess(value, _) => value.copy(copyright = (js \ "copyright").asOpt[JsObject].map(Json.fromJson[Copyright](_) match {
        case JsSuccess(copyValue, _) => Some(copyValue)
        case _ => None
      }).flatten)
      case _ => throw new ConversionException(new Error(metadataParseError("contributorDetails")))
    })
  }

  private def taskInfo(implicit metadata: Option[JsValue]): Option[TaskInfo] = {
    implicit val taskInfoReads = TaskInfo.taskInfoReads
    metadata.map(md => (md \ "taskInfo").asOpt[JsObject]).flatten.map(Json.fromJson[TaskInfo](_) match {
      case JsSuccess(value, _) => value
      case _ => throw new ConversionException(new Error(metadataParseError("taskInfo")))
    })
  }

  private def workflow(implicit metadata: Option[JsValue]): Option[Workflow] = {
    import org.corespring.platform.core.models.item.Workflow._
    metadata.map(md => (md \ "workflow").asOpt[Seq[String]] match {
      case Some(workflowStrings) => Some(Workflow(
        setup = workflowStrings.contains(setup),
        tagged = workflowStrings.contains(tagged),
        standardsAligned = workflowStrings.contains(standardsAligned),
        qaReview = workflowStrings.contains(qaReview)
      ))
      case _ => None
    }).flatten
  }

  private def supportingMaterials(itemId: VersionedId[ObjectId])(implicit metadata: Option[JsValue], sources: Map[String, Source]): Either[Error, Seq[Resource]] = {
    try {
      Right(metadata.map(md => (md \ "supportingMaterials").asOpt[Seq[JsObject]]).flatten.getOrElse(Seq.empty)
        .map(material => {
        val name = (material \ "name").asOpt[String].getOrElse("")
        val filenames = (material \ "files").asOpt[Seq[JsObject]].getOrElse(Seq.empty).map(f => (f \ "name").asOpt[String]).flatten
        upload(itemId, files = sources.filter{ case(filename, source) => filenames.contains(filename) }) match {
          case Right(files) => Resource(name = name, files = files)
          case Left(error) => throw new ConversionException(error)
        }
      }))
    } catch {
      case e: ConversionException => Left(e.error)
    }

  }
}

trait Uploader {
  def upload(filename: String, path: String, file: Source): Future[StoredFile]
}

class TransferManagerUploader(credentials: AWSCredentials, bucket: String) extends Uploader {

  val transferManager = new TransferManager(credentials)

  def upload(filename: String, path: String, file: Source) = future {
    val byteArray = file.map(_.toByte).toArray
    val metadata = new ObjectMetadata()
    metadata.setContentLength(byteArray.length)
    val result = transferManager.upload(bucket, path, new ByteArrayInputStream(byteArray), metadata).waitForUploadResult
    StoredFile(name = filename, contentType = BaseFile.getContentType(filename), storageKey = result.getKey)
  }

}