package web.controllers

import play.api.mvc.{Action, Controller}
import models.Item
import common.models.{SupportingMaterialHtml, SupportingMaterialFile, SupportingMaterial}
import org.bson.types.ObjectId

object Runner extends Controller {

  def getResource(itemId:String,materialName:String,resourceName:String) = Action{


    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {

        item.supportingMaterials.find( f => f.name == materialName ) match {
          case Some(matchingMaterial) => {
            val html : SupportingMaterialHtml = matchingMaterial.asInstanceOf[SupportingMaterialHtml]
             html.files.find( r => r.name == resourceName ) match {
               case Some(resource) => {
                 Ok(resource.content).withHeaders(("Content-Type","text/html"))
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
