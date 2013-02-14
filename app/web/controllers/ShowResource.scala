package web.controllers

import play.api.mvc._
import models._
import org.bson.types.ObjectId
import controllers.S3Service
import com.typesafe.config.ConfigFactory
import web.views.html.partials._edit._metadata._formWithLegend
import scala.Some
import scala.Some
import common.controllers.ItemResources
import xml.Elem
import testplayer.controllers.QtiRenderer
import controllers.auth.BaseApi
import qti.models.RenderingMode._

trait ObjectIdParser {

  def objectId(id: String): Option[ObjectId] = {
    try {
      Some(new ObjectId(id))
    }
    catch {
      case e: Exception => None
    }
  }
}


object ShowResource extends BaseApi with ObjectIdParser with ItemResources with QtiRenderer {

  def javascriptRoutes = Action{ implicit request =>

    import play.api.Routes
    import web.controllers.routes.javascript.{ ShowResource => ShowResourceJs }
    import web.controllers.routes.javascript._

    Ok(
      Routes.javascriptRouter("WebRoutes")(
      ShowResourceJs.renderDataResource,
      Partials.createItem,
      Partials.editItem,
      Partials.home,
      Partials.viewItem
    )).as("text/javascript")
  }

  /**
   * Render the Item.data resource using the CSS for printing.
   * @param itemId
   * @return
   */
  def renderDataResourceForPrinting(itemId: String) = {
    renderDataResource(itemId, renderMode = Printing)
  }

  /**
   * Render the Item.data resource. Find the default file in the Resource.files Seq,
   * If the file is qti.xml then redirect to the testplayer otherwise render as html.
   * @param itemId
   * @param toPrint if true the printing stylesheet will be used to render the resource
   * @return
   */
  def renderDataResource(itemId: String, renderMode: RenderingMode = Web) =
    objectId(itemId) match {
      case Some(oid) => {
        Item.findOneById(oid) match {
          case Some(item) => {

            item.data.get.files.find(_.isMain == true) match {
              case Some(defaultFile) => {

                def isQti = defaultFile.contentType == BaseFile.ContentTypes.XML && defaultFile.name == Resource.QtiXml

                def renderPlayer = ApiAction {
                  request =>
                    getItemXMLByObjectId(itemId, request.ctx.organization) match {
                      case Some(xmlData: Elem) => {
                        val finalXml = prepareQti(xmlData, renderMode)
                        Ok(testplayer.views.html.itemPlayer(itemId, finalXml, previewEnabled = (renderMode == Web), sessionId = "", request.token))
                      }
                      case None => NotFound("Can't find item")
                    }
                }

                if (isQti) {
                  renderPlayer()
                } else {
                  getDataFile(itemId, defaultFile.name)
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
  def renderResource(itemId: String, resourceName: String) =
    objectId(itemId) match {
      case Some(oid) => {
        Item.findOneById(oid) match {
          case Some(item) => {

            item.supportingMaterials.find(_.name == resourceName) match {
              case Some(resource) => {
                resource.files.find(_.isMain == true) match {
                  case Some(defaultFile) => {
                    //TODO: Is there a better way of doing this instead of redirecting?
                    val url = web.controllers.routes.ShowResource.getResourceFile(itemId, resource.name, defaultFile.name).url
                    Action(Redirect(url))
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
   * Return an individual file from a supporting material resource.
   * @param itemId
   * @param resourceName
   * @param filename
   * @return
   */
  def getResourceFile(itemId: String, resourceName: String, filename: String) = Action {
    request =>
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          item.supportingMaterials.find(f => f.name == resourceName) match {
            case Some(foundResource) => getResult(request, foundResource.files, filename)
            case _ => NotFound
          }
        }
        case _ => NotFound
      }
  }

}
