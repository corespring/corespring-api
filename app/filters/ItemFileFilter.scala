package filters

import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.player.cdn.CdnResolver
import org.corespring.v2.sessiondb.SessionServices
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{ Filter, RequestHeader, SimpleResult }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.matching.Regex

/**
 * A request to a player item file at
 * /v2/player/player/session/[sessionId]/filename
 * is redirected to cdn
 *
 * When the player is launched with isSecure=true
 * the index.html will be at /v2/player/player/session/index.html
 * and all images will have an url relative to that.
 */
trait ItemFileFilter extends Filter {

  type Req2Res = RequestHeader => Future[SimpleResult]

  def cdnResolver: CdnResolver

  lazy val logger = Logger(classOf[ItemFileFilter])

  def sessionServices: SessionServices

  implicit def ec: ExecutionContext

  private def redirectToCdn(sessionId: String, file: String) = Future {
    (for {
      json <- sessionServices.main.load(sessionId).orElse(sessionServices.preview.load(sessionId))
      itemId <- (json \ "itemId").asOpt[String]
      vid <- VersionedId(itemId)
      if vid.version.isDefined
      url <- Some(s"/${vid.id}/${vid.version.get}/data/$file")
      resolvedUrl <- Some(cdnResolver.resolveDomain(url))
    } yield {
      Redirect(resolvedUrl)
    }).getOrElse(BadRequest)
  }

  private def isOnCdn(file: String) = {
    file != "index.html"
  }

  private lazy val urlPattern = new Regex("/v2/player/player/session/([^/]*)/(.*)$")

  override def apply(f: Req2Res)(rh: RequestHeader): Future[SimpleResult] = {
    (rh.method, rh.path) match {
      case ("GET", urlPattern(sessionId, file)) if isOnCdn(file) => redirectToCdn(sessionId, file)
      case _ => f(rh)
    }
  }
}
