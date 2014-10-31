package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.corespring.common.url.BaseUrl
import org.corespring.importing.ItemFileConverter
import org.corespring.qtiToV2.SourceWrapper
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.services.OrgService
import org.corespring.v2.errors.V2Error
import play.api.mvc._
import play.api.templates.Html

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
        .map(orgService.defaultCollection(_)).flatten)) match {
      case (Some(upload), Success(Some(collectionId))) => {
        val zip = new ZipFile(upload.ref.file)
        val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
          (entry.getName -> SourceWrapper(Source.fromInputStream(zip.getInputStream(entry))("ISO-8859-1")))
        }).toMap

        val results = converter.convert(collectionId.toString)(fileMap)
        Ok(Html(results.map(_ match {
          case Failure(error) => error.getMessage
          case Success(item) => s"""<a href="http://localhost:9000/web#/edit/${item.id.toString}?panel=content" target="blank">${item.id.toString}</a>"""
        }).mkString("\n")))
      }
      case (None, _) => BadRequest("You need a file")
      case (_, _) => BadRequest("Not logged in")
    }
  }

  override def getOrgIdAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = userSession(request)

}