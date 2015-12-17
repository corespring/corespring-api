package web.controllers

import org.apache.commons.httpclient.util.URIUtil
import org.corespring.amazon.s3.S3Service
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.Item
import org.corespring.models.item.resource.{ BaseFile, Resource, StoredFile, VirtualFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.web.common.controllers.DefaultCss
import play.api.mvc.{ Action, AnyContent, Controller }
import play.mvc.Http.HeaderNames
import web.controllers.ShowResource.Errors

import scalaz.Scalaz._
import scalaz.{ Failure, Success }

object ShowResource {
  private[ShowResource] object Errors {
    val cantFindItem = "Can't find item"
    val cantFindResource = "Can't find resource"
    val invalidObjectId = "Invalid object id"
    val cantFindFileWithName = "Can't find file with name "
    val cantRender = "Unable to render"
    val noFilenameSpecified = "No filename specified"
  }
}

class ShowResource(itemService: ItemService, s3Service: S3Service, bucket: Bucket)
  extends Controller {

  def getResourceFile(itemId: String, resourceName: String, filename: String) = getFile(itemId, resourceName, Some(filename))

  private def getFile(itemId: String, resourceName: String, filename: Option[String] = None): Action[AnyContent] =
    {
      val decodedFilename = filename.map { n =>
        URIUtil.decode(n, "utf-8")
      }
      val out = for {
        oid <- VersionedId(itemId).toSuccess(Errors.invalidObjectId)
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

  private def getResource(item: Item, name: String): Option[(Boolean, Resource)] = if (name == Resource.DataPath) {
    item.data.map((true, _))
  } else {
    item.supportingMaterials.find(_.name == name).map((false, _))
  }

  def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = {
    Some(renderBaseFile(f))
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
          Ok(text).withHeaders((HeaderNames.CONTENT_TYPE, vFile.contentType))
        }
        case sFile: StoredFile => {
          s3Service.download(bucket.bucket, sFile.storageKey, Some(request.headers))
        }
      }
  }

  protected def addDefaultCss(html: String): String = {
    val css = Seq(DefaultCss.BOOTSTRAP, DefaultCss.UBUNTU, DefaultCss.DEFAULT_CSS).mkString("\n")
    val replacement = "<head>\n%s".format(css)
    """<head>""".r.replaceAllIn(html, replacement)
  }

  def renderDataResourceForPrinting(itemId: String) = Action(NotImplemented)
  def getDefaultResourceFile(itemId: String, resourceName: String) = Action(BadRequest)

  def javascriptRoutes = Action {
    implicit request =>

      import play.api.Routes
      import web.controllers.routes.javascript.{ Partials => PartialsJs, ShowResource => ShowResourceJs }
      Ok(
        Routes.javascriptRouter("WebRoutes")(
          PartialsJs.editItem,
          ShowResourceJs.getResourceFile)).as("text/javascript")
  }

}
