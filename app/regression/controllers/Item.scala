package regression.controllers

import play.api.mvc.Action
import org.bson.types.ObjectId
import player.accessControl.models.{RenderOptions, RequestedAccess}
import org.corespring.platform.data.mongo.models.VersionedId
import player.controllers.Views
import controllers.auth.TokenizedRequestActionBuilder
import models.item.service.{ItemServiceImpl, ItemService}
import player.accessControl.auth.CheckSessionAccess
import player.views.models.PlayerParams

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess], override val itemService : ItemService) extends Views(auth, itemService) {

  def Secured[A](username: String, password: String)(action: Action[A]) = Action(action.parser) { request =>
    request.headers.get("Authorization").flatMap { authorization =>
      authorization.split(" ").drop(1).headOption.filter { encoded =>
        new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
          case u :: p :: Nil if u == username && password == p => true
          case _ => false
        }
      }.map(_ => action(request))
    }.getOrElse {
      Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
    }
  }

  def cookies(orgId: ObjectId, itemId: VersionedId[ObjectId]) = Secured("admin", "1234secret") {
    Action { implicit request =>
      val newCookies : Seq[(String,String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.Preview)
      val newSession = sumSession(request.session, newCookies : _*)
      Ok.withSession(newSession)
    }
  }

  def playerWithLocalAssets(p: PlayerParams): play.api.templates.Html = player.views.html.LocalPlayer(p)

  override def preview(itemId: VersionedId[ObjectId]) = {
    val p = RenderParams(itemId, sessionMode = RequestedAccess.Mode.Preview, templateFn = playerWithLocalAssets)
    renderItem(p)
  }

}

object Item extends Item(CheckSessionAccess, ItemServiceImpl)