package web.controllers

import play.api.mvc._
import models._
import org.bson.types.ObjectId
import controllers.S3Service
import com.typesafe.config.ConfigFactory
import web.views.html.partials._edit._metadata._formWithLegend
import scala.Some
import scala.Some


trait ObjectIdParser{

  def objectId(id:String):Option[ObjectId] = {
    try{
      Some(new ObjectId(id))
    }
    catch {
      case e : Exception => None
    }
  }
}


object ShowResource extends Controller with ObjectIdParser{


  private val MOCK_ACCESS_TOKEN : String = "34dj45a769j4e1c0h4wb"

  private final val AMAZON_ASSETS_BUCKET : String =
   ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  private val ContentType : String = "Content-Type"


  /**
   * Render the Item.data resource. Find the default file in the Resource.files Seq,
   * If the file is qti.xml then redirect to the testplayer otherwise render as html.
   * @param itemId
   * @return
   */
  def renderDataResource(itemId:String) =
    objectId(itemId) match {
      case Some(oid) => {
        Item.findOneById(oid) match {
          case Some(item) => {

            item.data.get.files.find( _.isMain == true ) match {
              case Some(defaultFile) => {
                if (defaultFile.contentType == BaseFile.ContentTypes.XML && defaultFile.name == "qti.xml" ){
                  Action(Redirect("/testplayer/item/" + itemId + "?access_token=" + MOCK_ACCESS_TOKEN))
                } else {
                  Action(Redirect("/web/show-resource/" + itemId + "/data/" + defaultFile.name))
                }
              }
              case _ => Action(NotFound)
            }
          }
          case None => Action(NotFound)
        }
      }
      case None => Action(BadRequest("Invalid Object Id"))
  }

  /**
   * Given an item id and an resourceName - renders the SupportingMaterial resource.
   * This means that the default file in the files array will be rendered.
   * @param itemId
   * @param resourceName
   * @return
   */
  def renderResource(itemId:String, resourceName:String) =
    objectId(itemId) match {
      case Some(oid) => {
        Item.findOneById(oid) match {
          case Some(item) => {

            item.supportingMaterials.find( _.name == resourceName ) match {
              case Some(resource) => {
                resource.files.find(_.isMain == true) match {
                  case Some(defaultFile) => {
                    //TODO: Is there a better way of doing this?
                    Action(Redirect("/web/show-resource/" + item.id + "/" + resource.name + "/" + defaultFile.name))
                  }
                  case None => throw new RuntimeException("Bad data - no default file specified")
                }
              }
              case None => Action(NotFound(resourceName))
            }
          }
          case None => Action(NotFound)
        }
      }
      case None => Action(BadRequest("Invalid Object Id"))

  }

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


  /**
   * Return an individual file from a supporting material resource.
   * @param itemId
   * @param resourceName
   * @param filename
   * @return
   */
  def getResourceFile(itemId:String,resourceName:String,filename:String) = Action{ request =>
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
