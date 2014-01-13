package org.corespring.platform.core.controllers

import org.bson.types.ObjectId
import org.corespring.assets.{ S3ServiceClient, CorespringS3ServiceExtended, CorespringS3Service }
import org.corespring.common.config.AppConfig
import org.corespring.common.log.PackageLogging
import org.corespring.common.mongo.ObjectIdParser
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ StoredFile, VirtualFile, BaseFile, Resource }
import org.corespring.platform.core.models.itemSession.DefaultItemSession
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.services.item.ItemServiceClient
import org.corespring.web.common.controllers.DefaultCss
import play.api.mvc.Results._
import play.api.mvc._
import scalaz.Scalaz._
import scalaz.{ Success, Failure }

object AssetResource {
  object Errors {
    val cantFindItem = "Can't find item"
    val cantFindResource = "Can't find resource"
    val invalidObjectId = "Invalid object id"
    val cantFindFileWithName = "Can't find file with name "
    val cantRender = "Unable to render"
    val noFilenameSpecified = "No filename specified"
  }
}

trait AssetResource extends AssetResourceBase {
  final def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(renderBaseFile(f))
  def s3Service: CorespringS3Service = CorespringS3ServiceExtended
}

trait AssetResourceBase extends ObjectIdParser with S3ServiceClient with ItemServiceClient with PackageLogging {

  import AssetResource.Errors

  protected val ContentType: String = "Content-Type"

  def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]]

  def getDataFileBySessionId(sessionId: String, filename: String) = {

    DefaultItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => {
        val itemIdString = session.itemId.toString()
        getDataFile(itemIdString, filename)
      }
      case _ => Action(NotFound("sessionId: " + sessionId))
    }
  }

  def getDataFile(itemId: String, filename: String) = getFile(itemId, Resource.QtiPath, Some(filename))

  def renderDataResource(itemId: String) = getDefaultResourceFile(itemId, Resource.QtiPath)

  protected def addDefaultCss(html: String): String = {
    val css = Seq(DefaultCss.BOOTSTRAP, DefaultCss.UBUNTU, DefaultCss.DEFAULT_CSS).mkString("\n")
    val replacement = "<head>\n%s".format(css)
    """<head>""".r.replaceAllIn(html, replacement)
  }

  def getDefaultResourceFile(itemId: String, resourceName: String) = getFile(itemId, resourceName)

  def getResourceFile(itemId: String, resourceName: String, filename: String) = getFile(itemId, resourceName, Some(filename))

  private def getFile(itemId: String, resourceName: String, filename: Option[String] = None): Action[AnyContent] =
    {
      import VersionedIdImplicits.Binders._
      val decodedFilename = filename.map(java.net.URI.create(_).getPath)
      val out = for {
        oid <- stringToVersionedId(itemId).toSuccess(Errors.invalidObjectId)
        item <- itemService.findOneById(oid).toSuccess(Errors.cantFindItem)
        dr <- getResource(item, resourceName).toSuccess(Errors.cantFindResource)
        (isItemDataResource, resource) = dr
        name <- (decodedFilename orElse resource.defaultFile.map(_.name)).toSuccess(Errors.noFilenameSpecified)
        file <- resource.files.find(_.name == name).toSuccess(Errors.cantFindFileWithName + name)
        action <- renderFile(item, isItemDataResource, file).toSuccess(Errors.cantRender)
      } yield action

      out match {
        case Success(a) => a
        case Failure(e) => Action(BadRequest(e))
      }
    }

  private def getResource(item: Item, name: String): Option[(Boolean, Resource)] = if (name == Resource.QtiPath) {
    item.data.map((true, _))
  } else {
    item.supportingMaterials.find(_.name == name).map((false, _))
  }

  protected def renderBaseFile(f: BaseFile): Action[AnyContent] = Action {
    request =>
      f match {
        case vFile: VirtualFile => {
          val text = if (vFile.isMain && vFile.contentType == BaseFile.ContentTypes.HTML) {
            addDefaultCss(vFile.content)
          } else {
            vFile.content
          }
          Ok(text).withHeaders((ContentType, vFile.contentType))
        }
        case sFile: StoredFile => {
          s3Service.download(AppConfig.assetsBucket, sFile.storageKey, Some(request.headers))
        }
      }
  }

}
