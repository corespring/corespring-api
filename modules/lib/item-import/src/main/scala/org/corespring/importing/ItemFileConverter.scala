package org.corespring.importing

import scala.io.Source
import play.api.libs.json._
import com.fasterxml.jackson.core.JsonParseException
import org.corespring.platform.core.models.item._
import org.corespring.json.validation.JsonValidator
import org.corespring.platform.core.models.item.resource.BaseFile
import play.api.libs.json.JsSuccess
import scala.Some
import play.api.libs.json.JsObject
import org.corespring.amazon.s3.S3Service
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import play.api.libs.json.JsSuccess
import scala.Some
import org.corespring.v2.auth.models.OrgAndOpts
import play.api.libs.json.JsObject

trait ItemFileConverter {

  def s3: S3Service
  def bucket: String
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
              val item = Item(
                id = id,
                collectionId = Some(collectionId),
                contributorDetails = contributorDetails,
                lexile = extractString("lexile"),
                otherAlignments = otherAlignments,
                pValue = extractString("pValue"),
                playerDefinition =
                  Some(PlayerDefinition(files(id), (itemJson \ "xhtml").as[String], (itemJson \ "components"),
                    (itemJson \ "summaryFeedback").asOpt[String].getOrElse(""), None)),
                priorGradeLevels = extractStringSeq("priorGradeLevels"),
                priorUse = extractString("priorUse"),
                taskInfo = taskInfo,
                reviewsPassed = extractStringSeq("reviewsPassed"),
                standards = extractStringSeq("standards"),
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
    val definition = PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None)
    val item = Item(
      collectionId = Some(collectionId),
      playerDefinition = Some(definition))
    auth.insert(item)(identity)
  }

  private def files(itemId: VersionedId[ObjectId])(implicit sources: Map[String, Source]): Seq[BaseFile] = {
//    sources.toList.filterNot(f => Seq(itemJsonFilename, itemMetadataFilename).contains(f._1))
//      .map{ case(filename, source) => {
//        s3.upload(bucket, s"$itemId/data/$filename")
//      }}
    Seq.empty
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
    import Workflow._
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
}
