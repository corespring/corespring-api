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

object ItemFileConverter {

  object errors {
    def fileMissing(filename: String) = s"Provided item source did not include $filename"
    def jsonParseError(filename: String) = s"$filename did not contain valid json"
    def metadataParseError(field: String) = s"There was an error parsing $field in $itemMetadataFilename"
  }

  case class ImportException(error: Error) extends RuntimeException

  import errors._

  val itemJsonFilename = "item.json"
  val itemMetadataFilename = "metadata.json"

  val dummyReturn = Right(PlayerDefinition(Seq(), "<div>I'm a new item</div>", Json.obj(), "", None))


  def convert(collectionId: String)(implicit sources: Map[String, Source]): Either[Error, Item] = {
    try {
      (itemJson, metadata) match {
        case (Left(error), _) => Left(error)
        case (_, Left(error)) => Left(error)
        case (Right(item), Right(md)) => {
          implicit val metadata = md
          Right(Item(
            collectionId = Some(collectionId),
            contributorDetails = contributorDetails,
            lexile = extractString("lexile"),
            otherAlignments = otherAlignments,
            pValue = extractString("pValue"),
            playerDefinition =
              Some(PlayerDefinition(files, (item \ "xhtml").as[String], (item \ "components"),
                (item \ "summaryFeedback").asOpt[String].getOrElse(""), None)),
            priorGradeLevels = extractStringSeq("priorGradeLevels"),
            priorUse = extractString("priorUse"),
            taskInfo = taskInfo,
            reviewsPassed = extractStringSeq("reviewsPassed"),
            standards = extractStringSeq("standards"),
            workflow = workflow
          ))
        }
      }
    } catch {
      case ie: ImportException => Left(ie.error)
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

  private def files(implicit sources: Map[String, Source]): Seq[BaseFile] = {
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
      case _ => throw new ImportException(new Error(metadataParseError("otherAlignments")))
    })
  }

  private def contributorDetails(implicit metadata: Option[JsValue]): Option[ContributorDetails] = {
    implicit val contributorDetailsReads = ContributorDetails.Reads
    metadata.map(md => (md \ "contributorDetails").asOpt[JsObject]).flatten.map(Json.fromJson[ContributorDetails](_) match {
      case JsSuccess(value, _) => value
      case _ => throw new ImportException(new Error(metadataParseError("contributorDetails")))
    })
  }

  private def taskInfo(implicit metadata: Option[JsValue]): Option[TaskInfo] = {
    implicit val taskInfoReads = TaskInfo.taskInfoReads
    metadata.map(md => (md \ "taskInfo").asOpt[JsObject]).flatten.map(Json.fromJson[TaskInfo](_) match {
      case JsSuccess(value, _) => value
      case _ => throw new ImportException(new Error(metadataParseError("taskInfo")))
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
