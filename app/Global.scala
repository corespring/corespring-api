import com.mongodb.casbah.Imports
import com.mongodb.casbah.commons.MongoDBObject
import com.novus.salat._
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.lti.web.accessControl.auth.QuerySessionRenderOptions
import org.corespring.platform.core.models.item.{ Item, ItemTransformationCache, PlayItemTransformationCache }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.player.accessControl.auth.CheckSessionAccess
import org.corespring.qtiToV2.transformers.ItemTransformer

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import actors.reporting.ReportActor
import akka.actor.Props
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import common.seed.SeedDb
import common.seed.SeedDb._
import filters.{ AccessControlFilter, AjaxFilter, Headers, IEHeaders }
import org.bson.types.ObjectId
import org.corespring.api.v1.{ dependencies, ItemApi }
import org.corespring.common.log.ClassLogging
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.services.{ BaseFindAndSaveService, UserServiceWired }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.play.utils._
import org.corespring.reporting.services.ReportGenerator
import org.corespring.v2.api.Bootstrap
import org.corespring.v2.player.V2PlayerIntegration
import org.corespring.web.common.controllers.deployment.{ AssetsLoaderImpl, LocalAssetsLoaderImpl }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.mvc.Results._

object Global
  extends WithFilters(CallBlockOnHeaderFilter, AjaxFilter, AccessControlFilter, IEHeaders)
  with ControllerInstanceResolver
  with GlobalSettings
  with ClassLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  val INIT_DATA: String = "INIT_DATA"

  private lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.getString("components.path").toSeq

    val showReleasedOnlyComponents: Boolean = containerConfig.getBoolean("components.showReleasedOnly")
      .getOrElse { Play.current.mode == Mode.Prod }

    val out = new FileComponentLoader(path, showReleasedOnlyComponents)
    out.reload
    out
  }

  def containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeKey <- if (current.mode == Mode.Prod) Some("prod") else Some("non-prod")
      modeConfig <- container.getConfig(modeKey)
    } yield modeConfig
  }.getOrElse(Configuration.empty)

  lazy val itemTransformer = new ItemTransformer {
    def cache: ItemTransformationCache = PlayItemTransformationCache
    def itemService: BaseFindAndSaveService[Item, VersionedId[ObjectId]] = new BaseFindAndSaveService[Item, VersionedId[ObjectId]] {

      override def findOneById(id: VersionedId[Imports.ObjectId]): Option[Item] = ItemServiceWired.findOneById(id)

      /**
       * Note: Whilst we support v1 and v2 players, we need to allow the item transformer to save 'versioned' items (aka not the most recent item).
       * This is not possible using the SalatVersioningDao as it has logic in place that prevents that happening.
       * @param i
       * @param createNewVersion
       * @see SalatVersioningDao
       */
      override def save(i: Item, createNewVersion: Boolean): Unit = {
        import com.mongodb.casbah.Imports._
        import org.corespring.platform.core.models.mongoContext.context
        logger.debug(s"[itemTransformer.save] - saving versioned content directly")
        val dbo: MongoDBObject = new MongoDBObject(grater[Item].asDBObject(i))
        ItemServiceWired.dao.currentCollection.save(dbo)
      }
    }
  }

  //TODO - there is some crossover between V2PlayerIntegration and V2ApiBootstrap - should they be merged
  lazy val integration = new V2PlayerIntegration(
    componentLoader.all,
    containerConfig,
    SeedDb.salatDb(),
    itemTransformer)

  lazy val v2ApiBootstrap = new Bootstrap(
    ItemServiceWired,
    Organization,
    AccessToken,
    integration.sessionService,
    UserServiceWired,
    integration.secureSocialService,
    integration.itemAuth,
    integration.sessionAuth,
    itemTransformer,
    integration.getOrgIdAndOptions)

  def controllers: Seq[Controller] = integration.controllers ++ v2ApiBootstrap.controllers

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

    Future {
      InternalServerError(org.corespring.web.common.views.html.onError(uid, throwable))
    }
  }

  private def applyFilter(f: Future[SimpleResult]): Future[SimpleResult] = f.map(_.withHeaders(Headers.AccessControlAllowEverything))

  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Future[SimpleResult] = applyFilter(super.onHandlerNotFound(request))

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): Future[SimpleResult] = applyFilter(super.onBadRequest(request, error))

  override def onStart(app: Application): Unit = {

    CallBlockOnHeaderFilter.block = (rh: RequestHeader) => {

      if (componentLoader != null && rh.path.contains("/v2/player") && rh.path.endsWith("player")) {
        logger.info("reload components!")
        componentLoader.reload

        if (componentLoader.all.length == 0) {
          throw new RuntimeException("No components loaded - check your component path configuration: 'components.path'")
        }
      }
    }

    integration.validate match {
      case Left(err) => throw new RuntimeException(err)
      case Right(_) => Unit
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
        seedStaticData()
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

