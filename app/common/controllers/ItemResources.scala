package common.controllers

import play.api.mvc.Results._
import models._
import item.{Content, Item}
import item.resource.{StoredFile, VirtualFile, BaseFile, Resource}
import play.api.mvc.{Result, AnyContent, Request, Action}
import org.bson.types.ObjectId
import scala.Some
import controllers.{ConcreteS3Service, S3Service}
import web.controllers.utils.ConfigLoader
import xml.Elem
import controllers.auth.Permission

trait ItemResources {

  val AMAZON_ASSETS_BUCKET = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get
  private val ContentType: String = "Content-Type"

  /**
   * Return an individual file from Item.data
   * TODO: This is an unprotected resource. We'll want to protect it, but
   * I think it needs some thought. using access tokens will break the html client.
   * @param itemId
   * @param filename
   * @return
   */
  def getDataFile(itemId: String, filename: String) = Action {
    request =>
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          item.data match {
            case Some(foundData) => {
              getResult(request, foundData.files, filename)
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


  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  def getItemXMLByObjectId(itemId: String, callerOrg: ObjectId): Option[Elem] = {
    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {
        if (Content.isCollectionAuthorized(callerOrg, item.collectionId, Permission.Read)) {
          val dataResource = item.data.get

          dataResource.files.find(_.name == Resource.QtiXml) match {
            case Some(qtiXml) => {
              Some(scala.xml.XML.loadString(qtiXml.asInstanceOf[VirtualFile].content))
            }
            case _ => None
          }
        } else None
      }
      case _ => None
    }
  }


  private def addDefaultCss(html : String) : String ={
    val css = Seq(DefaultCss.BOOTSTRAP, DefaultCss.UBUNTU, DefaultCss.DEFAULT_CSS).mkString("\n")
    val replacement = "<head>\n%s".format(css)
    """<head>""".r.replaceAllIn(html, replacement )
  }

}
