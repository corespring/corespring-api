package web.controllers

import org.corespring.legacy.ServiceLookup
import org.corespring.models.{ Organization, User }
import play.api.{ Play, Mode }
import play.api.mvc.{ Controller, Action }
import org.corespring.common.config.AppConfig

object Partials extends Controller with securesocial.core.SecureSocial {

  def editItem = SecuredAction { request =>
    val userId = request.user.identityId
    val user: User = ServiceLookup.userService.getUser(userId.userId, userId.providerId).getOrElse(throw new RuntimeException("Unknown user"))
    val useV2 = Play.current.mode == Mode.Dev || AppConfig.v2playerOrgIds.contains(user.org.orgId)
    val isRoot = AppConfig.rootOrgId == user.org.orgId
    Ok(web.views.html.partials.editItem(useV2, isRoot))
  }

  def redirect(url: String) = Action {
    if (Play.current.mode == Mode.Dev) {
      Redirect("/login")
    } else {
      MovedPermanently(url)
    }
  }

  def loadFromPath(path: String) = Action {

    val fullName = s"web.views.html.${path.replace("/", ".")}"

    try {
      val c: Class[_] = Play.current.classloader.loadClass(fullName)
      println(s"c: $c")
      val fn = c.getMethod("render")
      Ok(fn.invoke(c).asInstanceOf[play.api.templates.HtmlFormat.Appendable])
    } catch {
      case t: Throwable => {
        t.printStackTrace()
        BadRequest(s"Can't find class with name $path [$fullName]")
      }
    }

  }

}
