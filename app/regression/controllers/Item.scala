package regression.controllers

import play.api.mvc.Action
import org.bson.types.ObjectId
import player.accessControl.models.{RequestedAccess, RenderOptions}
import player.controllers.Views
import org.corespring.platform.core.models.item.service.{ItemServiceImpl, ItemService}
import player.accessControl.auth.{TokenizedRequestActionBuilder, CheckSessionAccess}
import common.controllers.deployment.LocalAssetsLoaderImpl
import java.util.NoSuchElementException
import play.api.libs.json.Json._
import scala.Some
import org.corespring.platform.data.mongo.models.VersionedId
import api.ApiError
import org.corespring.platform.core.models.itemSession.{ItemSessionCompanion, DefaultItemSession}

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess], override val itemService : ItemService, itemSession: ItemSessionCompanion)
  extends Views(auth, itemService) {

  private val USER = "admin"
  private val PASSWORD = "1234secret"

  def BasicHttpAuth[A](action: Action[A]) = Action(action.parser) { request =>
    request.headers.get("Authorization").flatMap { authorization =>
      authorization.split(" ").drop(1).headOption.filter { encoded =>
        new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
          case u :: p :: Nil if u == USER && PASSWORD == p => true
          case _ => false
        }
      }.map(_ => action(request))
    }.getOrElse {
      Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
    }
  }

  def simplePlayer(requestedAccess: String, orgId: ObjectId, itemId: VersionedId[ObjectId]) = BasicHttpAuth {
    try {
      val access = RequestedAccess.Mode.withName(requestedAccess)
      val params = RenderParams(itemId, sessionMode = access, assetsLoader = LocalAssetsLoaderImpl)
      renderSimplePlayer(params, orgId)
    } catch {
      case e: NoSuchElementException => Action { NotFound }
    }
  }

  def renderPlayer(orgId: ObjectId, itemSessionId: ObjectId) = BasicHttpAuth {
    itemSession.findOneById(itemSessionId) match {
      case Some(itemSession) => {
        val params = RenderParams(itemSession.itemId, sessionMode = RequestedAccess.Mode.Render, sessionId = Some(itemSessionId), assetsLoader = LocalAssetsLoaderImpl)
        renderSimplePlayer(params, orgId)
      }
      case None => {
        Action { BadRequest(toJson(ApiError.ItemSessionNotFound)) }
      }
    }
  }

  private def renderSimplePlayer(params: RenderParams, orgId: ObjectId) = Action {
    implicit request => {
      prepareHtml(params, orgId).map{ html =>
        val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(params.sessionMode)
        val newSession = sumSession(request.session, newCookies: _*)
        Ok(html).withSession(newSession)
      }.getOrElse(NotFound)
    }
  }

}

object Item extends Item(CheckSessionAccess, ItemServiceImpl, DefaultItemSession)