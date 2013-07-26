package common.controllers

import common.config.AppConfig
import common.log.PackageLogging
import controllers.{CorespringS3Service, CorespringS3ServiceImpl, S3ServiceClient}
import models.item.Item
import models.item.resource.{StoredFile, VirtualFile, BaseFile, Resource}
import models.item.service.ItemServiceClient
import models.itemSession.DefaultItemSession
import org.bson.types.ObjectId
import play.api.mvc.Results._
import play.api.mvc._
import scalaz.Scalaz._
import scalaz.{Success, Failure}
import web.controllers.ObjectIdParser


object AssetResource{
  object Errors{
    val cantFindItem = "Can't find item"
    val cantFindResource = "Can't find resource"
    val invalidObjectId = "Invalid object id"
    val cantFindFileWithName = "Can't find file with name "
    val cantRender = "Unable to render"
    val noFilenameSpecified = "No filename specified"
  }
}

trait AssetResource extends AssetResourceBase{
  final def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(renderBaseFile(f))
  def s3Service : CorespringS3Service = CorespringS3ServiceImpl
}


trait AssetResourceBase extends ObjectIdParser with S3ServiceClient with ItemServiceClient with PackageLogging{

  import AssetResource.Errors

  protected val ContentType: String = "Content-Type"

  def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]]

  def getDataFileBySessionId(sessionId: String, filename: String) = {
    DefaultItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => getDataFile(session.itemId.toString, filename)
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

  def getDefaultResourceFile(itemId:String,resourceName:String) = getFile(itemId, resourceName)

  def getResourceFile(itemId: String, resourceName: String, filename:String ) = getFile(itemId,resourceName,Some(filename))

  private def getFile(itemId:String, resourceName:String, filename: Option[String] = None) : Action[AnyContent] =
  {
    import models.versioning.VersionedIdImplicits.Binders._

    val out = for {
      oid <- stringToVersionedId(itemId).toSuccess(Errors.invalidObjectId)
      item <- itemService.findOneById(oid).toSuccess(Errors.cantFindItem)
      dr <- getResource(item, resourceName).toSuccess(Errors.cantFindResource)
      (isItemDataResource, resource) = dr
      name <- (filename orElse resource.defaultFile.map(_.name)).toSuccess(Errors.noFilenameSpecified)
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
    request => f match {
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
