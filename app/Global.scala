package global

import bootstrap.Main
import filters.Headers
import filters._
import org.corespring.play.utils.{ CallBlockOnHeaderFilter, ControllerInstanceResolver }
import org.joda.time.DateTime
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.{ Future, ExecutionContext }
import ExecutionContext.Implicits.global

object Global
  extends WithFilters(
    CallBlockOnHeaderFilter,
    AjaxFilter,
    AccessControlFilter,
    IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings {

  private lazy val logger = Logger(Global.getClass)

  lazy val main = Main(Play.current)
  lazy val controllers: Seq[Controller] = main.controllers

  override def onStart(app: Application): Unit = {

    CallBlockOnHeaderFilter.block = (rh: RequestHeader) => {

      if (app.mode != Mode.Prod &&
        main.componentLoader != null &&
        rh.path.contains("/v2/player") &&
        rh.path.endsWith("player")) {
        logger.info("reload components!")
        main.componentLoader.reload

        if (main.componentLoader.all.length == 0) {
          throw new RuntimeException("No components loaded - check your component path configuration: 'components.path'")
        }
      }
    }

    main.assetsLoader.init()
  }

  override def doFilter(a: EssentialAction): EssentialAction = {
    Filters(super.doFilter(a), Seq(main.componentSetFilter) ++ main.itemFileFilter: _*)
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(Action(new play.api.mvc.Results.Status(200)))
      case _ => {
        main.apiTracking.handleRequest(request)
        super.onRouteRequest(request)
      }
    }
  }

  private def applyFilter(f: Future[SimpleResult]): Future[SimpleResult] = f.map(_.withHeaders(Headers.AccessControlAllowEverything))

  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Future[SimpleResult] = applyFilter(super.onHandlerNotFound(request))

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): Future[SimpleResult] = applyFilter(super.onBadRequest(request, error))

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {

    val uid = s"${DateTime.now.getMillis()}-${scala.util.Random.alphanumeric.take(4).mkString}"
    logger.error(uid)
    logger.error(throwable.getMessage)

    if (logger.isWarnEnabled) {
      throwable.printStackTrace()
    }

    Future {
      if (request.accepts(ContentTypes.JSON)) {
        InternalServerError(Json.obj("error" -> throwable.getMessage, "uid" -> uid))
      } else {
        InternalServerError(org.corespring.web.common.views.html.onError(uid, throwable))
      }
    }
  }
}

