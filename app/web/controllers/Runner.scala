package web.controllers

import play.api.mvc._
import models._
import org.bson.types.ObjectId
import controllers.S3Service
import com.typesafe.config.ConfigFactory
import web.views.html.partials._edit._metadata._formWithLegend
import scala.Some
import scala.Some


object Runner extends Controller {

  private final val AMAZON_ASSETS_BUCKET : String =
   ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  private val ContentType : String = "Content-Type"


  def getDataResource(itemId:String, filename:String) = Action{ request =>
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


  def getResource(itemId:String,resourceName:String,filename:String) = Action{ request =>
    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {
        item.supportingMaterials.find( f => f.name == resourceName ) match {
          case Some(foundResource) =>  getResult(request, foundResource.files, filename)
          case _ => NotFound
        }
      }
      case _ => NotFound
    }
  }

  private def getResult(request : Request[AnyContent], files :Seq[BaseFile], filename:String) : Result = {
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
