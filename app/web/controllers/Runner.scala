package web.controllers

import play.api.mvc.{Action, Controller}
import models.{StoredFile, VirtualFile, Item}
import org.bson.types.ObjectId
import controllers.S3Service
import com.typesafe.config.ConfigFactory



object Runner extends Controller {

  private final val AMAZON_ASSETS_BUCKET : String =
   ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")


  def getResource(itemId:String,materialName:String,resourceName:String) = Action{ request =>


    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {

        item.supportingMaterials.find( f => f.name == materialName ) match {
          case Some(foundResource) => {
             foundResource.files.find( r => r.name == resourceName ) match {
               case Some(foundFile) => {
                 foundFile match {
                  case vFile : VirtualFile => {
                     Ok(vFile.content).withHeaders(("Content-Type",vFile.contentType))
                  }
                  case sFile : StoredFile => {
                    S3Service.download(AMAZON_ASSETS_BUCKET, sFile.storageKey, Some(request.headers))
                  }
                 }
               }
               case _ => NotFound
             }
          }
          case _ => NotFound
        }
      }
      case _ => NotFound
    }
   
  }

}
