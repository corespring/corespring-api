package org.corespring.container.client.controllers.apps

import org.corespring.container.client.V2PlayerConfig
import org.corespring.container.client.component.{ ComponentBundler, ItemComponentTypes }
import org.corespring.container.client.controllers.GetAsset
import org.corespring.container.client.hooks.Hooks.StatusMessage
import org.corespring.container.client.hooks.PlayerHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.client.pages.PlayerRenderer
import org.corespring.container.components.services.ComponentService
import play.api.Mode
import play.api.Mode.Mode
import play.api.http.ContentTypes
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.templates.Html
import play.api.cache.Cache
import org.corespring.container.client.controllers.apps.routes.{ Player => PlayerRoutes }

import scala.concurrent.Future

case class CdnConfig(host: String)

class Player(mode: Mode,
  bundler: ComponentBundler,
  val containerContext: ContainerExecutionContext,
  playerRenderer: PlayerRenderer,
  componentService: ComponentService,
  val hooks: PlayerHooks,
  playerConfig: V2PlayerConfig)
  extends Controller
  with GetAsset[PlayerHooks]
  with QueryStringHelper
  with PlayerSkinHelper {

  private def handleSuccess[D](fn: (D) => Future[SimpleResult])(e: Either[StatusMessage, D]): Future[SimpleResult] = e match {
    case Left((code, msg)) => Future {
      Status(code)(msg)
    }
    case Right(s) => fn(s)
  }

  private def createPlayerHtml(sessionId: String, session: JsValue, item: JsValue, defaults: JsValue)(implicit r: RequestHeader): Either[String, Future[Html]] = {

    val ids = ItemComponentTypes(componentService, item).map(_.id)

    val prodMode = r.getQueryString("mode").map(_ == "prod").getOrElse(mode == Mode.Prod)
    val queryParams = mkQueryParams(mapToJson)
    val encodedComputedColors = calculateColorToken(queryParams, defaults)
    val computedIconSet = calculateIconSet(queryParams, defaults)
    val colors = calculateColors(queryParams, defaults)

    bundler.bundle(ids, "player", Some("player"), !prodMode, Some(encodedComputedColors)) match {
      case Some(b) => {
        val hasBeenArchived = hooks.archiveCollectionId == (session \ "collectionId")
        val showControls = r.getQueryString("showControls").map(_ == "true").getOrElse(false)
        val queryParams = mkQueryParams(m => m)

        val warnings: Seq[String] = if (hasBeenArchived) {
          Seq("Warning: This item has been deleted")
        } else {
          Nil
        }

        Right(
          playerRenderer.render(sessionId, session, item, b, warnings, queryParams, prodMode, showControls, computedIconSet, colors))
      }
      case _ => Left(s"Failed to create a bundle for: $sessionId")
    }
  }

  private def loadSessionAndItem(sessionId: String)(implicit rh: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue, JsValue)]] = {
    import play.api.Play.current
    val key = s"loadSessionAndItem_$sessionId"
    val v: Option[JsObject] = Cache.get(key).map(_.asInstanceOf[JsObject])

    println(s"[loadSessionAndItem] $key - cached? ${v.isDefined}")

    v match {
      case Some(obj) => Future.successful(
        Right(
          (obj \ "session").as[JsValue],
          (obj \ "item").as[JsValue],
          (obj \ "defaults").as[JsValue]))

      case _ => {

        hooks.loadSessionAndItem(sessionId).map { result =>
          {
            result match {
              case Left(e) => Left(e)
              case Right(d) => {
                val (session, item, defaults) = d
                println(s"set cache: $key")
                Cache.set(key, Json.obj(
                  "session" -> session,
                  "item" -> item,
                  "defaults" -> defaults), 5)
                Right(d)
              }
            }

          }
        }
      }
    }
  }

  private def getLocation(call: Call, request: RequestHeader) = {
    val arr: Array[String] = call.url.split("\\?")
    val Array(baseUrl, extras) = arr
    val extraParams = toMap(extras)
    val params = mkQueryParams[String](m => {
      mapToParamString(extraParams ++ m)
    })(request)
    s"${baseUrl}${if (params.isEmpty) "" else s"?$params"}"
  }

  def loadBySession(sessionId: String) = Action.async { implicit request =>
    loadSessionAndItem(sessionId).flatMap {
      handleSuccess { (tuple) =>
        val (session, _, _) = tuple
        val itemId = (session \ "itemId").as[String]
        val sessionId = (session \ "id").asOpt[String]
        import org.corespring.container.client.controllers.apps.routes.Player
        lazy val call = Player.load(itemId, sessionId)
        lazy val location = getLocation(call, request)
        Future.successful(SeeOther(location))
      }
    }
  }

  def load(itemId: String, sessionId: Option[String]) = Action.async { implicit request =>

    sessionId match {
      case None => Future(NotFound("No sessionId"))
      case Some(s) => {
        loadSessionAndItem(s).flatMap {
          handleSuccess { (tuple) =>
            val (session, item, defaults) = tuple
            require((session \ "id").asOpt[String].isDefined, "The session model must specify an 'id'")
            createPlayerHtml(s, session, item, defaults) match {
              case Left(e) => Future.successful(BadRequest(e))
              case Right(f) => f.map(Ok(_))
            }
          }
        }

      }
    }
  }

  /**
   * show a simple submit button
   * showControls=true|false (default: false)
   * - show simple player controls (for devs)
   */
  val showControls = "showControls"

  /**
   * A set of player query string params, that should be set on the player, but can be removed therafter
   */
  override val paramsToStrip = showControls +: StrippableParams.params

  def createSessionForItem(itemId: String): Action[AnyContent] = Action.async { implicit request =>

    hooks.createSessionForItem(itemId).flatMap {
      handleSuccess { (tuple) =>
        val (session, item, defaults) = tuple
        require((session \ "id").asOpt[String].isDefined, "The session model must specify an 'id'")
        createPlayerHtml((session \ "id").as[String], session, item, defaults) match {
          case Left(e) => Future.successful(BadRequest(e))
          case Right(f) => f.map { html =>

            lazy val call = PlayerRoutes.load(itemId, (session \ "id").asOpt[String])
            lazy val location = getLocation(call, request)
            Created(html)
              .as(ContentTypes.HTML)
              .withHeaders(LOCATION -> location)
          }
        }
      }
    }
  }

  def getFileByItemId(itemId: String, file: String) = Action.async {
    implicit request =>
      {
        val isCloudFront = request.headers.toMap.keys.exists(k => k.startsWith("CloudFront"))
        if (isCloudFront) {
          Future(hooks.loadItemFile(itemId, file)(request))
        } else {
          playerConfig.cdn match {
            case Some(c) => {
              val download = request.queryString.get("dl").map(_.head == "true").getOrElse(false)
              if (download) {
                Future(hooks.loadItemFile(itemId, file)(request))
              } else {
                val call = PlayerRoutes.getFileByItemId(itemId, file)
                val url = s"${call.url}?dl=true"
                Future.successful(SeeOther(s"${c.host}${url}"))
              }
            }
            case _ => Future(hooks.loadItemFile(itemId, file))
          }
        }
      }
  }
}
