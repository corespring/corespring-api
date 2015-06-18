import actors.reporting.ReportActor
import akka.actor.Props
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import filters.{AccessControlFilter, AjaxFilter, Headers, IEHeaders}
import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.play.utils._
import org.corespring.reporting.services.ReportGenerator
import org.corespring.web.common.controllers.deployment.{AssetsLoaderImpl, LocalAssetsLoaderImpl}
import org.corespring.wiring.AppWiring
import org.corespring.wiring.sessiondb.DynamoSessionDbInitialiser
import org.joda.time.{DateTime, DateTimeZone}
import play.api._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

object Global
  extends WithFilters(CallBlockOnHeaderFilter, AjaxFilter, AccessControlFilter, IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings
  with ClassLogging {

  import scala.concurrent.ExecutionContext.Implicits.global


  def controllers: Seq[Controller] = AppWiring.controllers

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(Action(new play.api.mvc.Results.Status(200)))
      case _ => {
        AppWiring.apiTracking.handleRequest(request)
        super.onRouteRequest(request)
      }
    }
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {

    val uid = new ObjectId().toString
    logger.error(uid)
    logger.error(throwable.getMessage)

    if (logger.isDebugEnabled) {
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

  private def applyFilter(f: Future[SimpleResult]): Future[SimpleResult] = f.map(_.withHeaders(Headers.AccessControlAllowEverything))

  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Future[SimpleResult] = applyFilter(super.onHandlerNotFound(request))

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): Future[SimpleResult] = applyFilter(super.onBadRequest(request, error))

  override def onStart(app: Application): Unit = {

    CallBlockOnHeaderFilter.block = (rh: RequestHeader) => {

      if (AppWiring.componentLoader != null && rh.path.contains("/v2/player") && rh.path.endsWith("player")) {
        logger.info("reload components!")
        AppWiring.componentLoader.reload

        if (AppWiring.componentLoader.all.length == 0) {
          throw new RuntimeException("No components loaded - check your component path configuration: 'components.path'")
        }
      }
    }

    AppWiring.validate match {
      case Left(err) => throw new RuntimeException(err)
      case Right(_) => Unit
    }

    RegisterJodaTimeConversionHelpers()

    AssetsLoaderImpl.init(app)
    LocalAssetsLoaderImpl.init(app)
    DynamoSessionDbInitialiser.init(app)
  }

  private def timeLeftUntil2am = {
    implicit val postfixOps = scala.language.postfixOps
    // Looks like Play is not adjusting the timezone properly here, so it's set for 7am, which ought to be
    // 6am UTC -> 2am EDT
    (new DateTime().plusDays(1).withTimeAtStartOfDay().plusHours(6).withZone(DateTimeZone.forID("America/New_York"))
      .getMinuteOfDay + 1 - new DateTime().withZone(DateTimeZone.forID("America/New_York")).getMinuteOfDay) minutes
  }

  private def reportingDaemon(app: Application) = {
    import scala.language.postfixOps

    Logger.info("Scheduling the reporting daemon")

    val reportingActor = Akka.system(app).actorOf(Props(classOf[ReportActor], ReportGenerator))
    Akka.system(app).scheduler.schedule(timeLeftUntil2am, 24 hours, reportingActor, "reportingDaemon")
  }


}
