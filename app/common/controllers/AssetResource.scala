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


trait AssetResource extends AssetResourceBase {
  final def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(renderBaseFile(f))
  def service : S3Service = ConcreteS3Service
}


trait AssetResourceBase extends ObjectIdParser with S3ServiceModule {

  val AMAZON_ASSETS_BUCKET = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get
  protected val ContentType: String = "Content-Type"

  def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]]

  def getDataFileBySessionId(sessionId: String, filename: String) = {
    DefaultItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => getDataFile(session.itemId.toString, filename)
      case _ => Action(NotFound("sessionId: " + sessionId))
    }
  }

  def getDataFile(itemId: String, filename: String) = getResourceFile(itemId, Resource.QtiPath, filename)

  def renderDataResource(itemId: String) = renderResource(itemId, Resource.QtiPath)

  protected def addDefaultCss(html: String): String = {
    val css = Seq(DefaultCss.BOOTSTRAP, DefaultCss.UBUNTU, DefaultCss.DEFAULT_CSS).mkString("\n")
    val replacement = "<head>\n%s".format(css)
    """<head>""".r.replaceAllIn(html, replacement)
  }

  def renderResource(itemId: String, resourceName: String): Action[AnyContent] = {
    val out = for {
      oid <- objectId(itemId).toSuccess("Invalid objectId")
      item <- Item.findOneById(oid).toSuccess("Can't find Item")
      dr <- getResource(item, resourceName).toSuccess("Can't find resource")
      action <- renderUsingDefaultFile(item, dr._1, dr._2).toSuccess("Can't render this resource")
    } yield action

    out match {
      case Success(a) => a
      case Failure(e) => Action(BadRequest(e))
    }
  }


  def getResourceFile(itemId: String, resourceName: String, filename: String): Action[AnyContent] = {
    val out = for {
      oid <- objectId(itemId).toSuccess("Invalid objectId")
      item <- Item.findOneById(oid).toSuccess("Can't find Item")
      dr <- getResource(item, resourceName).toSuccess("Can't find resource")
      file <- dr._2.files.find(_.name == filename).toSuccess("Can't find file with name " + filename)
      action <- renderFile(item, dr._1, file).toSuccess("Can't render file")
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

  private def renderUsingDefaultFile(item: Item, isDataResource: Boolean, r: Resource): Option[Action[AnyContent]] = r.defaultFile.map(renderFile(item, isDataResource, _)).getOrElse(None)


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
