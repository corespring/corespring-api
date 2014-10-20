package org.corespring.importing.controllers

import java.util.zip.ZipFile

import org.corespring.importing.ItemFileConverter
import org.corespring.platform.core.controllers.auth.{ApiRequest, BaseApi}
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.{AuthMode, PlayerAccessSettings, OrgAndOpts}
import play.api.mvc._
import scala.collection.JavaConversions._
import scala.io.Source

class ItemImportController(converter: ItemFileConverter) extends BaseApi {

  def uploadForm() = Action {
    Ok(org.corespring.importing.views.html.uploadForm.render())
  }

  def upload() = ApiAction(parse.multipartFormData) { request =>
    (request.body.file("file"), Organization.getDefaultCollection(request.ctx.organization)) match {
      case (Some(upload), Right(collection)) => {
        val zip = new ZipFile(upload.ref.file)
        val fileMap = zip.entries.filterNot(_.isDirectory).map(entry => {
          (entry.getName -> Source.fromInputStream(zip.getInputStream(entry))("ISO-8859-1"))
        }).toMap
        converter.convert(collection.id.toString, getOpts(request))(fileMap) match {
          case Left(error) => BadRequest(error.getMessage)
          case Right(item) => Ok(item.id.toString)
        }
      }
      case (None, _) => BadRequest("You need a file")
      case (_, Left) => BadRequest("Not logged in")
    }
  }

  private def getOpts(request: ApiRequest[_]): OrgAndOpts =
    OrgAndOpts(orgId = request.ctx.organization, opts = PlayerAccessSettings.ANYTHING, authMode = AuthMode.UserSession)

}