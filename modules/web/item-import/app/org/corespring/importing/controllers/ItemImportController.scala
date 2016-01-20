package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.corespring.importing.ItemFileConverter
import org.corespring.services.{ OrgCollectionService }
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.io.Source
import scalaz._

class ItemImportController(
  converter: ItemFileConverter,
  userSession: UserSessionOrgIdentity,
  orgCollectionService: OrgCollectionService) extends Controller {

  def uploadForm() = Action {
    Ok(org.corespring.importing.views.html.uploadForm())
  }

  def upload() = Action(parse.multipartFormData) { request =>

    lazy val defaultCollection = getOrgAndOptions(request)
      .map(opts => orgCollectionService.getDefaultCollection(opts.org.id))

    (request.body.file("file"), defaultCollection) match {
      case (Some(upload), Success(collectionId)) => {
        val zip = new ZipFile(upload.ref.file)
        val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
          (entry.getName -> Source.fromInputStream(zip.getInputStream(entry))("ISO-8859-1"))
        }).toMap
        converter.convert(collectionId.toString)(fileMap) match {
          case Success(item) => Ok(item.id.toString)
          case Failure(error) => BadRequest(error.getMessage)
        }
      }
      case (None, _) => BadRequest("You need a file")
      case (_, _) => BadRequest("Not logged in")
    }
  }

  private def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

}