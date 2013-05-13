package common.controllers

import controllers.{S3Service, S3ServiceModule, ConcreteS3Service}
import models.item.Item
import models.item.resource.{StoredFile, VirtualFile, BaseFile, Resource}
import models.itemSession.DefaultItemSession
import org.bson.types.ObjectId
import play.api.mvc.Results._
import play.api.mvc._
import scala.Some
import scalaz.Scalaz._
import scalaz.{Success, Failure}
import web.controllers.ObjectIdParser
import web.controllers.utils.ConfigLoader


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

trait AssetResource extends AssetResourceBase {
  final def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(renderBaseFile(f))
  def service : S3Service = ConcreteS3Service
}


trait AssetResourceBase extends ObjectIdParser with S3ServiceModule {

  import AssetResource.Errors

  val AMAZON_ASSETS_BUCKET = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get
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
    val out = for {
      oid <- objectId(itemId).toSuccess(Errors.invalidObjectId)
      item <- Item.findOneById(oid).toSuccess(Errors.cantFindItem)
      (isItemDataResource,resource) <- getResource(item, resourceName).toSuccess(Errors.cantFindResource)
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
        service.download(AMAZON_ASSETS_BUCKET, sFile.storageKey, Some(request.headers))
      }
    }
  }

}
