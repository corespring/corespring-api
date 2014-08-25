package web.controllers

import play.api.{ Play, Mode }
import play.api.mvc.Action
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.data.mongo.models.VersionedId
import org.bson.types.ObjectId
import org.corespring.platform.core.services.item.{ItemServiceWired, ItemService}
import org.corespring.platform.core.models.versioning.VersionedIdImplicits

class Partials(service: ItemService) extends BaseApi {

  def editItem(itemId: String) = ApiAction { request =>
    val isLatest = isLatestVersion(itemId)
    val useV2 = Play.current.mode == Mode.Dev || AppConfig.v2playerOrgIds.contains(request.ctx.organization)
    Ok(web.views.html.partials.editItem(useV2, isLatest))
  }

  def createItem = Action { Ok(web.views.html.partials.createItem()) }
  def home = Action { Ok(web.views.html.partials.home()) }
  def viewItem = Action { Ok(web.views.html.partials.viewItem()) }

  private def isLatestVersion(itemId: String) = {
    import VersionedIdImplicits.Binders._
    stringToVersionedId(itemId) match {
      case Some(id) => service.findOneById(id.copy(version = None)) match {
        case Some(item) => item.id.version == id.version
        case None => true
      }
      case None => true
    }
  }

}

object Partials extends Partials(ItemServiceWired)
