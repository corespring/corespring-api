package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.bson.types.ObjectId
import org.corespring.importing.{ItemImporterExporter, ItemFileConverter}
import org.corespring.platform.core.models.{JsonUtil, ContentCollection}
import org.corespring.platform.core.utils.CsvWriter
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.qtiToV2.kds.PathFlattener
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc._

import scala.collection.JavaConversions._
import scalaz._

class ItemImportController(exporter: ItemImporterExporter,
                           converter: ItemFileConverter,
                           userSession: UserSessionOrgIdentity[OrgAndOpts],
                           orgService: OrgService) extends LoadOrgAndOptions with Controller with CsvWriter
with JsonUtil {

  import PathFlattener._

  val CsvFilename = "imported-files.csv"
  val ZipFilename = "corespring-json.zip"

  def uploadForm() = Action { request =>
    getOrgAndOptions(request).map(_.org.contentcolls.map(_.collectionId)).map(ContentCollection.get(_)) match {
      case Success(collections) => Ok(org.corespring.importing.views.html.uploadForm.render(collections))
      case _ => InternalServerError("There was a problem rendering the uploader. Are you logged in?")
    }
  }

  def upload() = Action(parse.multipartFormData) { request =>
    val metadata = getMetadata(request)
    (request.body.file("file"), getCollection(request)) match {
      case (Some(upload), Success(Some(collection))) => {
        val zip = new ZipFile(upload.ref.file)
        val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
          entry.getName.flattenPath -> SourceWrapper(entry.getName, zip.getInputStream(entry))
        }).toMap
        getAction(request) match {
          case "export" => exportZip(collection, metadata, fileMap)
          case _ =>  importToDb(collection.id, metadata, fileMap, request.host)
        }
      }
      case (_, Failure(message)) => BadRequest(message)
      case (None, _) => BadRequest("You need a file")
      case (_, _) => BadRequest("Not logged in")
    }
  }

  private def importToDb(collectionId: ObjectId, metadata: JsObject, fileMap: Map[String, SourceWrapper], host: String) = {
    val results = converter.convert(collectionId.toString, metadata)(fileMap)
    Ok((List("Item ID", "URL") +: results.map(_ match {
      case Failure(error) => List(error.getMessage)
      case Success(item) => List(item.id.toString, s"http://$host/web#/edit/${item.id.toString}?panel=content")
    })).toList.toCsv).withHeaders(("Content-Type", "text/csv"), ("Content-Disposition", s"attachment; file=$CsvFilename"))
  }

  private def exportZip(collection: ContentCollection, metadata: JsObject, fileMap: Map[String, SourceWrapper]) =
    Ok(exporter.export(collection, metadata, fileMap)).withHeaders(
      "Content-Type" -> "application/zip",
      "Content-Disposition" -> s"attachment; filename=$ZipFilename"
    )

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

  private def getMetadata(request: Request[MultipartFormData[Files.TemporaryFile]]) = partialObj(
    "scoringType" -> getParamOpt("scoring-type", request).map(JsString(_))
  )

  private def getAction(request: Request[MultipartFormData[Files.TemporaryFile]]) =
    request.body.asFormUrlEncoded.get("action").map(_.headOption).flatten.getOrElse("export")

  private def getParamOpt(key: String, request: Request[MultipartFormData[Files.TemporaryFile]]): Option[String] = {
    request.body.asFormUrlEncoded.get(key).map(_.headOption).flatten
  }

  private def getCollection(request: Request[MultipartFormData[Files.TemporaryFile]]) =
    request.body.asFormUrlEncoded.get("collectionId").map(_.headOption).flatten match {
      case Some(collectionId) => Success(ContentCollection.get(Seq(new ObjectId(collectionId))).headOption)
      case _ => Failure("Could not find collectionId in request")
    }

}