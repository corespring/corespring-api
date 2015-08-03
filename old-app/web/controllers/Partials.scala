package web.controllers

import org.corespring.models.{ User, Organization }
import play.api.templates.{ Html, Template0 }
import play.api.{ Play, Mode }
import play.api.mvc.Action
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.auth.BaseApi
import play.templates.BaseScalaTemplate

object Partials extends BaseApi {

  def editItem = SecuredAction { request =>
    val user: User = User.getUser(request.user.identityId).getOrElse(throw new RuntimeException("Unknown user"))
    val useV2 = Play.current.mode == Mode.Dev || AppConfig.v2playerOrgIds.contains(user.org.orgId)
    val isRoot = Organization.findOneById(user.org.orgId).map(_.isRoot).getOrElse(false)
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
