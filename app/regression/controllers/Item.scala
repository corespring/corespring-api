package regression.controllers

import play.api.mvc.Action
import org.bson.types.ObjectId
import player.accessControl.models.{RequestedAccess, RenderOptions}
import org.corespring.platform.data.mongo.models.VersionedId
import player.controllers.Views
import controllers.auth.TokenizedRequestActionBuilder
import models.item.service.{ItemServiceImpl, ItemService}
import player.accessControl.auth.CheckSessionAccess
import common.controllers.deployment.LocalAssetsLoaderImpl
import java.util.NoSuchElementException

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess], override val itemService : ItemService)
  extends Views(auth, itemService) {

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

  def simplePlayer(requestedAccess: String, orgId: ObjectId, itemId: VersionedId[ObjectId]) = Secured("admin", "1234secret") {
    try {
      Action {
        implicit request => {
          val access = RequestedAccess.Mode.withName(requestedAccess)
          val params = RenderParams(itemId, sessionMode = access, assetsLoader = LocalAssetsLoaderImpl)
          prepareHtml(params, itemId, orgId).map{ html =>
            val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(access)
            val newSession = sumSession(request.session, newCookies: _*)
            Ok(html).withSession(newSession)
          }.getOrElse(NotFound)
        }
      }
    } catch {
      case e: NoSuchElementException => Action { NotFound }
    }
  }
}

object Item extends Item(CheckSessionAccess, ItemServiceImpl)