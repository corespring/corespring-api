package web.controllers

import org.corespring.assets.{ CorespringS3ServiceExtended, CorespringS3Service }
import org.corespring.common.mongo.ObjectIdParser
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.controllers.{QtiResource, AssetResourceBase}
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.resource.BaseFile
import org.corespring.platform.core.models.item.{ Item, Content }
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.core.services.item.{ ItemServiceClient, ItemService }
import org.corespring.player.v1.controllers.QtiRenderer
import org.corespring.player.v1.views.models.{QtiKeys, PlayerParams}
import org.corespring.qti.models.RenderingMode._
import play.api.mvc._
import scala.xml.Elem
import scalaz.Scalaz._
import scalaz.{ Success, Failure }

object ShowResource
  extends BaseApi
  with ObjectIdParser
  with QtiResource
  with ItemServiceClient
  with AssetResourceBase
  with QtiRenderer {

  def s3Service: CorespringS3Service = CorespringS3ServiceExtended

  def itemService: ItemService = ItemServiceWired

  def javascriptRoutes = Action {
    implicit request =>

      import play.api.Routes
      import web.controllers.routes.javascript.{ ShowResource => ShowResourceJs }

      Ok(
        Routes.javascriptRouter("WebRoutes")(
          ShowResourceJs.getResourceFile,
          Partials.createItem,
          Partials.editItem,
          Partials.home,
          Partials.viewItem)).as("text/javascript")
  }

  /**
   * Render the Item.data resource using the CSS for printing.
   * TODO: This doesn't support non-QTI data items.
   * It will have to at some point.
   * @param itemId
   * @return
   */
  def renderDataResourceForPrinting(itemId: String): Action[AnyContent] = {

    import VersionedIdImplicits.Binders._
    val out = for {
      oid <- stringToVersionedId(itemId).toSuccess("Invalid object id")
      item <- itemService.findOneById(oid).toSuccess("Can't find item id")
    } yield renderPlayer(item, Printing)

    out match {
      case Success(a) => a
      case Failure(e) => Action(BadRequest(e))
    }
  }

  private def renderPlayer(item: Item, renderMode: RenderingMode = Web): Action[AnyContent] =
    ApiAction {
      request =>


        if (Content.isAuthorized(request.ctx.organization, item.id, Permission.Read)) {

          itemService.getQtiXml(item.id) match {
            case Some(xmlData: Elem) => {
              val qtiKeys = QtiKeys((xmlData \ "itemBody")(0))
              val finalXml = prepareQti(xmlData, renderMode)
              val params: PlayerParams = PlayerParams(finalXml, itemId = Some(item.id.toString()), previewEnabled = (renderMode == Web), qtiKeys = qtiKeys, mode = renderMode)
              Ok(org.corespring.player.v1.views.html.Player(params))
            }
            case None => NotFound("Can't find item")
          }
        } else {
          BadRequest("Not Authorized")
        }
    }

  private def isQti(f: BaseFile) = f.contentType == BaseFile.ContentTypes.XML && f.name == Item.QtiResource.QtiXml

  override def renderFile(item: Item, isDataResource: Boolean, f: BaseFile): Option[Action[AnyContent]] = Some(
    if (isDataResource && isQti(f))
      renderPlayer(item)
    else
      renderBaseFile(f))

}
