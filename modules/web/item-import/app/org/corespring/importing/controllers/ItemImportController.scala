package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.corespring.importing.ItemFileConverter
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.io.Source
import scalaz._

class ItemImportController(converter: ItemFileConverter,
                           userSession: UserSessionOrgIdentity[OrgAndOpts],
                           orgService: OrgService) extends LoadOrgAndOptions with Controller {

  def uploadForm() = Action {
    Ok(org.corespring.importing.views.html.uploadForm.render())
  }

  def upload() = Action(parse.multipartFormData) { request =>

    (request.body.file("file"), getOrgIdAndOptions(request).map(_.orgId).map(orgService.org(_)
        .map(orgService.defaultCollection(_)))) match {
      case (Some(upload), Success(Some(collectionId))) => {
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

  override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

}