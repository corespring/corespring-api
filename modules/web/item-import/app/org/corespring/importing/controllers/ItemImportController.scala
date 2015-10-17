package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.corespring.importing.ItemFileConverter
import org.corespring.services.OrganizationService
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import play.api.mvc._

import scala.collection.JavaConversions._
import scala.io.Source
import scalaz._
import scalaz.Scalaz._

class ItemImportController(converter: ItemFileConverter,
  userSession: UserSessionOrgIdentity[OrgAndOpts],
  orgService: OrganizationService) extends LoadOrgAndOptions with Controller {

  def uploadForm() = Action {
    Ok(org.corespring.importing.views.html.uploadForm.render())
  }

  def upload() = Action(parse.multipartFormData) { request =>

    val item = for {
      orgAndOpts <- getOrgAndOptions(request).leftMap(_.message)
      upload <- request.body.file("file").toSuccess("You need a file")
      collection <- orgService.getOrCreateDefaultCollection(orgAndOpts.org.id).leftMap(_.message)
      zip = new ZipFile(upload.ref.file)
      val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
        (entry.getName -> Source.fromInputStream(zip.getInputStream(entry))("ISO-8859-1"))
      }).toMap
      item <- converter.convert(collection.id.toString)(fileMap).leftMap(_.getMessage)
    } yield item

    item match {
      case Success(i) => Ok(i.id.toString)
      case Failure(msg) => BadRequest(msg)
    }
  }

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

}