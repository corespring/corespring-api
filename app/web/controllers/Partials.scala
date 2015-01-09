package web.controllers

import org.corespring.platform.core.models.{ User, Organization }
import play.api.{ Play, Mode }
import play.api.mvc.Action
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.auth.BaseApi

object Partials extends BaseApi {

  def editItem = SecuredAction { request =>
    val user: User = User.getUser(request.user.identityId).getOrElse(throw new RuntimeException("Unknown user"))
    val useV2 = Play.current.mode == Mode.Dev || AppConfig.v2playerOrgIds.contains(user.org.orgId)
    val isRoot = Organization.findOneById(user.org.orgId).map(_.isRoot).getOrElse(false)
    Ok(web.views.html.partials.editItem(useV2, isRoot))
  }
  def createItem = Action { Ok(web.views.html.partials.createItem()) }
  def home = Action { Ok(web.views.html.partials.home()) }
  def viewItem = Action { Ok(web.views.html.partials.viewItem()) }
  def redirect(url: String) = Action { MovedPermanently(url) }
}
