import actors.reporting.ReportActor
import akka.actor.Props
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import common.seed.SeedDb
import common.seed.SeedDb._
import filters.{ IEHeaders, Headers, AjaxFilter, AccessControlFilter }
import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.play.utils._
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.poc.integration.ControllerInstanceResolver
import org.corespring.reporting.services.ReportGenerator
import org.corespring.v2player.integration.V2PlayerIntegration
import org.corespring.web.common.controllers.deployment.{ LocalAssetsLoaderImpl, AssetsLoaderImpl }
import org.joda.time.DateTime
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc.Results._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object Global
  extends WithFilters(CallBlockOnHeaderFilter, AjaxFilter, AccessControlFilter, IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings
  with ClassLogging {

  val INIT_DATA: String = "INIT_DATA"

  private lazy val componentLoader: ComponentLoader = {
    val out = new FileComponentLoader(Play.current.configuration.getString("components.path").toSeq)
    out.reload
    out
  }

  def controllers: Seq[Controller] = new V2PlayerIntegration(componentLoader.all, current.configuration, SeedDb.salatDb()).controllers

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(Action(new play.api.mvc.Results.Status(200)))
      case _ => {
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

    Future { InternalServerError(org.corespring.web.common.views.html.onError(uid, throwable)) }
  }

  private def applyFilter(f: Future[SimpleResult]): Future[SimpleResult] = f.map(_.withHeaders(Headers.AccessControlAllowEverything))

  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Future[SimpleResult] = applyFilter(super.onHandlerNotFound(request))

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): Future[SimpleResult] = applyFilter(super.onBadRequest(request, error))

  override def onStart(app: Application): Unit = {

    CallBlockOnHeaderFilter.block = (rh: RequestHeader) => {

      if (componentLoader != null && rh.path.contains("/v2/player") && rh.path.endsWith("player")) {
        logger.info("reload components!")
        componentLoader.reload
      }
    }

    RegisterJodaTimeConversionHelpers()

    AssetsLoaderImpl.init(app)
    LocalAssetsLoaderImpl.init(app)

    val initData: Boolean = app.configuration.getBoolean(INIT_DATA).getOrElse(false)

    logger.debug(s"Init Data: $initData :: ${app.configuration.getBoolean(INIT_DATA)}")

    def onlyIfLocalDb(fns: (() => Unit)*) {
      if (isSafeToSeedDb(app))
        fns.foreach(fn => fn())
      else
        throw new RuntimeException("You're trying to seed against a remote db - bad idea")
    }

    logger.debug(s"App mode: ${app.mode}")

    app.mode match {

      case Mode.Test => {
        //onlyIfLocalDb(emptyData, seedTestData)
      }
      case Mode.Dev => {
        if (initData) {
          onlyIfLocalDb(emptyData, seedDevData, seedDebugData, seedDemoData)
        }
        seedStaticData()
      }
      case Mode.Prod => {
        if (initData) {
          emptyData()
          seedDevData()
        }
        seedStaticData()
        seedDemoData()
        initializeReports
        reportingDaemon(app)
      }
    }

  }

  private def isSafeToSeedDb(implicit app: Application): Boolean = {
    val uri = app.configuration.getString("mongodb.default.uri")

    require(uri.isDefined, "the mongo uri isn't defined!")

    def isSafeRemoteUri(uri: String): Boolean = {
      val safeRemoteUri = app.configuration.getString("seed.db.safe.mongodb.uri")
      safeRemoteUri.map(safeUri => uri == safeUri).getOrElse(false)
    }

    uri.map { u => u.contains("localhost") || u.contains("127.0.0.1") || isSafeRemoteUri(u) }.getOrElse(false)
  }

  private def timeLeftUntilMidnight = {
    implicit val postfixOps = scala.language.postfixOps
    (new DateTime().plusDays(1).withTimeAtStartOfDay().minusMinutes(1).getMinuteOfDay + 1
      - new DateTime().getMinuteOfDay) minutes
  }

  private def reportingDaemon(app: Application) = {
    import scala.language.postfixOps

    Logger.info("Scheduling the reporting daemon")

    val reportingActor = Akka.system(app).actorOf(Props(classOf[ReportActor], ReportGenerator))
    Akka.system(app).scheduler.schedule(timeLeftUntilMidnight, 24 hours, reportingActor, "reportingDaemon")
  }

  /**
   * Add demo data models to the the db to allow end users to be able to
   * view the content as a demo.
   * This involves:
   * 1. adding a demo access token that is associated with a demo organization
   * 2. adding a demo organiztion
   *
   * TODO: the demo orgs listed are hardcoded
   */
  private def seedDemoData() {
    seedData("conf/seed-data/demo")
  }

  private def initializeReports() {
    ReportGenerator.generateAllReports
  }

  /* Data that needs to get seeded regardless of the INIT_DATA setting */
  private def seedStaticData() {
    emptyStaticData()
    seedData("conf/seed-data/static")
  }

  private def seedDevData() {
    seedData("conf/seed-data/common", "conf/seed-data/dev", "conf/seed-data/exemplar-content")
  }

  private def seedDebugData() {
    //do not call emptyData() as it expects to be called after seedDevData
    seedData("conf/seed-data/debug")
  }

}

