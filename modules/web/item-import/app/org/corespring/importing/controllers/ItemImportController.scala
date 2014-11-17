package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.bson.types.ObjectId
import org.corespring.importing.ItemFileConverter
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.utils.CsvWriter
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import play.api.libs.Files
import play.api.mvc._
import play.api.templates.Html

import scala.collection.JavaConversions._
import scalaz._

class ItemImportController(converter: ItemFileConverter,
                           userSession: UserSessionOrgIdentity[OrgAndOpts],
                           orgService: OrgService) extends LoadOrgAndOptions with Controller with CsvWriter {

  def uploadForm() = Action { request =>
    getOrgAndOptions(request).map(_.org.contentcolls.map(_.collectionId)).map(ContentCollection.get(_)) match {
      case Success(collections) => Ok(org.corespring.importing.views.html.uploadForm.render(collections))
      case _ => InternalServerError("There was a problem rendering the uploader. Are you logged in?")
    }
  }

  def upload() = Action(parse.multipartFormData) { request =>
    def defaultCollection = getOrgAndOptions(request).map(opts => orgService.defaultCollection(opts.org))

    (request.body.file("file"), getCollectionId(request)) match {
      case (Some(upload), Success(collectionId)) => {
        val zip = new ZipFile(upload.ref.file)
        val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
          (entry.getName -> SourceWrapper(zip.getInputStream(entry)))
        }).toMap

        val results = converter.convert(collectionId.toString)(fileMap)
        Ok((List("Item ID", "URL") +: results.map(_ match {
          case Failure(error) => List(error.getMessage)
          case Success(item) => List(item.id.toString, s"https://${request.host}/web#/edit/${item.id.toString}?panel=content")
        })).toList.toCsv).withHeaders(("Content-type", "text/csv"), ("Content-disposition", s"attachment; file=imported_files.csv"))
      }
      case (_, Failure(message)) => BadRequest(message)
      case (None, _) => BadRequest("You need a file")
      case (_, _) => BadRequest("Not logged in")
    }
  }

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

  private def getCollectionId(request: Request[MultipartFormData[Files.TemporaryFile]]) =
    request.body.asFormUrlEncoded.get("collectionId").map(_.headOption).flatten match {
      case Some(collectionId) => Success(new ObjectId(collectionId))
      case _ => Failure("Could not find collectionId in request")
    }

}