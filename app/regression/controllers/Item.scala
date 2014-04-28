package regression.controllers

import api.ApiError
import java.util.NoSuchElementException
import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.{ ItemSessionCompanion, DefaultItemSession }
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.{ CheckSessionAccess, TokenizedRequestActionBuilder }
import org.corespring.player.accessControl.models.{ RequestedAccess, RenderOptions }
import org.corespring.web.common.controllers.deployment.LocalAssetsLoaderImpl
import play.api.libs.json.Json._
import play.api.mvc.Action
import player.controllers.Views
import scala.Some
import org.corespring.platform.core.services.item.{ ItemServiceImpl, ItemService }
import scala.concurrent.{ExecutionContext, Future}

class Item(auth: TokenizedRequestActionBuilder[RequestedAccess], override val itemService: ItemService, itemSession: ItemSessionCompanion)
  extends Views(auth, itemService, AssessmentService) {

  import ExecutionContext.Implicits.global

  private val USER = "admin"
  private val PASSWORD = "1234secret"

  def BasicHttpAuth[A](action: Action[A]) = Action.async(action.parser) { request =>
    request.headers.get("Authorization").flatMap { authorization =>
      authorization.split(" ").drop(1).headOption.filter { encoded =>
        new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes)).split(":").toList match {
          case u :: p :: Nil if u == USER && PASSWORD == p => true
          case _ => false
        }
      }.map(_ => action(request))
    }.getOrElse {
      Future(Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured""""))
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
    implicit request =>
      {
        prepareHtml(params, orgId).map { html =>
          val newCookies: Seq[(String, String)] = playerCookies(orgId, Some(RenderOptions.ANYTHING)) :+ activeModeCookie(params.sessionMode)
          val newSession = sumSession(request.session, newCookies: _*)
          Ok(html).withSession(newSession)
        }.getOrElse(NotFound)
      }
  }

}

object Item extends Item(CheckSessionAccess, ItemServiceImpl, DefaultItemSession)