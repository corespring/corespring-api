package web.controllers

import play.api.{ Play, Mode }
import play.api.mvc.Action
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.auth.BaseApi

object Partials extends BaseApi {

  def editItem = ApiAction { request =>
    val useV2 = Play.current.mode == Mode.Dev || AppConfig.v2playerOrgIds.contains(request.ctx.organization)
    Ok(web.views.html.partials.editItem(useV2))
  }
  def createItem = Action { Ok(web.views.html.partials.createItem()) }
  def home = Action { Ok(web.views.html.partials.home()) }
  def viewItem = Action { Ok(web.views.html.partials.viewItem()) }
  def redirect(url: String) = Action { MovedPermanently(url) }
}
