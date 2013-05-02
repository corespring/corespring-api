package common.controllers

import play.api.mvc.Results._
import models._
import item.Item
import item.resource.{StoredFile, VirtualFile, BaseFile}
import play.api.mvc.{Result, AnyContent, Request, Action}
import org.bson.types.ObjectId
import scala.Some
import controllers.ConcreteS3Service
import web.controllers.utils.ConfigLoader
import models.itemSession.{DefaultItemSession, ItemSession}

trait AssetResource {

  val AMAZON_ASSETS_BUCKET = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get
  private val ContentType: String = "Content-Type"


  def getDataFileBySessionId(sessionId: String, filename: String) = {
    DefaultItemSession.findOneById(new ObjectId(sessionId)) match {
      case Some(session) => getDataFile(session.itemId.toString, filename)
      case _ => Action(NotFound("sessionId: " + sessionId))
    }
  }

  /**
   * Return an individual file from Item.data
   * @param itemId
   * @param filename
   * @return
   */
  def getDataFile(itemId: String, filename: String, dummy: String = "") = Action {
    request =>
      val fn = filename.substring(filename.lastIndexOf("/")+1)
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          item.data match {
            case Some(foundData) => {
              getResult(request, foundData.files, fn)
            }
            case _ => NotFound
          }
        }
        case _ => NotFound
      }
  }


  def getResult(request: Request[AnyContent], files: Seq[BaseFile], filename: String): Result = {
    files.find(_.name == filename) match {
      case Some(foundFile) => {
        foundFile match {
          case vFile: VirtualFile => {
            //TODO: is this the best place to be adding this?
            val text = if (vFile.isMain && vFile.contentType == BaseFile.ContentTypes.HTML) {
              addDefaultCss(vFile.content)
            } else {
              vFile.content
            }
            Ok(text).withHeaders((ContentType, vFile.contentType))
          }
          case sFile: StoredFile => {
            ConcreteS3Service.download(AMAZON_ASSETS_BUCKET, sFile.storageKey, Some(request.headers))
          }
        }
      }
      case _ => NotFound
    }
  }


  private def addDefaultCss(html: String): String = {
    val css = Seq(DefaultCss.BOOTSTRAP, DefaultCss.UBUNTU, DefaultCss.DEFAULT_CSS).mkString("\n")
    val replacement = "<head>\n%s".format(css)
    """<head>""".r.replaceAllIn(html, replacement)
  }

}
