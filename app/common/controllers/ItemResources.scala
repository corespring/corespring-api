package common.controllers

import play.api.mvc.Results._
import models._
import play.api.mvc.{Result, AnyContent, Request, Action}
import org.bson.types.ObjectId
import scala.Some
import controllers.S3Service
import web.controllers.utils.ConfigLoader

trait ItemResources {

  val AMAZON_ASSETS_BUCKET = ConfigLoader.get("AMAZON_ASSETS_BUCKET").get
  private val ContentType : String = "Content-Type"

  /**
   * Return an individual file from Item.data
   * @param itemId
   * @param filename
   * @return
   */
  def getDataFile(itemId:String, filename:String) = Action{ request =>
    Item.findOneById( new ObjectId(itemId)) match {
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


  def getResult(request : Request[AnyContent], files :Seq[BaseFile], filename:String) : Result = {
    files.find( _.name == filename) match {
      case Some(foundFile) => {
        foundFile match {
          case vFile : VirtualFile => {
            Ok(vFile.content).withHeaders((ContentType,vFile.contentType))
          }
          case sFile : StoredFile => {
            S3Service.download(AMAZON_ASSETS_BUCKET, sFile.storageKey, Some(request.headers))
          }
        }
      }
      case _ => NotFound
    }
  }

}
