package web.controllers

import common.controllers.{AssetResourceBase, AssetResource, QtiResource}
import controllers.auth.BaseApi
import play.api.mvc._
import player.controllers.QtiRenderer
import models.item.Item
import models.item.resource.{Resource, BaseFile}
import scala.xml.Elem
import player.views.models.PlayerParams
import qti.models.RenderingMode._
import scala.Some
import scalaz.Scalaz._
import scalaz.{Success, Failure}
import controllers.{ConcreteS3Service, S3Service}


object ShowResource extends BaseApi with ObjectIdParser with QtiResource with AssetResourceBase with QtiRenderer {

  def service : S3Service = ConcreteS3Service

  def javascriptRoutes = Action {
    implicit request =>

      import play.api.Routes
      import web.controllers.routes.javascript._
      import web.controllers.routes.javascript.{ShowResource => ShowResourceJs}

      Ok(
        Routes.javascriptRouter("WebRoutes")(
          ShowResourceJs.renderResource,
          Partials.createItem,
          Partials.editItem,
          Partials.home,
          Partials.viewItem
        )).as("text/javascript")
  }

  /** Render the Item.data resource using the CSS for printing.
    * TODO: This doesn't support not QTI base items data items.
    * It will have to at some point.
    * @param itemId
    * @return
    */
  def renderDataResourceForPrinting(itemId: String): Action[AnyContent] = {

    val out = for {
      oid <- objectId(itemId).toSuccess("Invalid object id")
      item <- Item.findOneById(oid).toSuccess("Can't find item id")
    } yield renderPlayer(item, Printing)

    out match {
      case Success(a) => a
      case Failure(e) => Action(BadRequest(e)
    }
  }

  private def renderPlayer(item: Item, renderMode: RenderingMode = Web): Action[AnyContent] =
    ApiAction {
      request =>
        getItemXMLByObjectId(item, request.ctx.organization) match {
          case Some(xmlData: Elem) => {
            val finalXml = prepareQti(xmlData, renderMode)
            val params: PlayerParams = PlayerParams(itemId = Some(item.id.toString), xml = finalXml, previewEnabled = (renderMode == Web))
            Ok(player.views.html.Player(params))
          }
          case None => NotFound("Can't find item")
        }
    }


  private def isQti(f: BaseFile) = f.contentType == BaseFile.ContentTypes.XML && f.name == Resource.QtiXml

  override def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(
    if (isDataResource && isQti(f))
      renderPlayer(item)
    else
      renderBaseFile(f)
  )


}
