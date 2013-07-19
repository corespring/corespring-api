package regression.controllers

import play.api.mvc.Action
import org.bson.types.ObjectId
import player.accessControl.models.{RenderOptions, RequestedAccess}
import org.corespring.platform.data.mongo.models.VersionedId
import player.controllers.Views
import controllers.auth.TokenizedRequestActionBuilder
import models.item.service.{ItemServiceImpl, ItemService}
import player.accessControl.auth.CheckSessionAccess
import common.controllers.deployment.LocalAssetsLoaderImpl
import play.api.templates.Html
import player.views.models.{QtiKeys, PlayerParams}
import qti.models.RenderingMode
import org.xml.sax.SAXParseException

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

  def simplePlayer(orgId: ObjectId, itemId: VersionedId[ObjectId]) = Secured("admin", "1234secret") {
    Action {
      implicit request =>
        prepareHtml(itemId, orgId).map{ html =>
          val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.Preview)
          val newSession = sumSession(request.session, newCookies: _*)
          Ok(html).withSession(newSession)
        }.getOrElse(NotFound)
    }
  }

  private def prepareHtml(itemId: VersionedId[ObjectId], orgId: ObjectId): Option[Html] = try {
    import models.versioning.VersionedIdImplicits.Binders._
    getItemXMLByObjectId(itemId, orgId).map {
      xmlData =>
        val params = PlayerParams(
          prepareQti(xmlData, RenderingMode.Web),
          Some(versionedIdToString(itemId)),
          None,
          true,
          QtiKeys((xmlData \ "itemBody")(0)),
          RenderingMode.Web)
        Some(player.views.html.Player(params))
    }.getOrElse(None)
  } catch {
    case e: SAXParseException => None
  }

  def cookies(orgId: ObjectId, itemId: VersionedId[ObjectId]) = Secured("admin", "1234secret") {
    Action { implicit request =>
      val newCookies : Seq[(String,String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(RequestedAccess.Mode.Preview)
      val newSession = sumSession(request.session, newCookies : _*)
      Ok.withSession(newSession)
    }
  }

  override def preview(itemId: VersionedId[ObjectId]) = {
    val p = RenderParams(itemId, sessionMode = RequestedAccess.Mode.Preview, assetsLoader = LocalAssetsLoaderImpl)
    renderItem(p)
  }

}

object Item extends Item(CheckSessionAccess, ItemServiceImpl)