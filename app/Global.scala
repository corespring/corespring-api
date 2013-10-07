import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import common.seed.SeedDb._
import filters.{ IEHeaders, Headers, AjaxFilter, AccessControlFilter }
import org.bson.types.ObjectId
import org.corespring.common.log.ClassLogging
import org.corespring.container.components.loader.{ComponentLoader, FileComponentLoader}
import org.corespring.poc.integration.ControllerInstanceResolver
import org.corespring.poc.integration.impl.PocIntegrationImpl
import org.corespring.web.common.controllers.deployment.{ LocalAssetsLoaderImpl, AssetsLoaderImpl }
import play.api._
import play.api.mvc.Results._
import play.api.mvc._

object Global extends WithFilters(AjaxFilter, AccessControlFilter, IEHeaders) with ControllerInstanceResolver with ClassLogging {

  val INIT_DATA: String = "INIT_DATA"

  private lazy val componentLoader : ComponentLoader = {
    val out = new FileComponentLoader(Play.current.configuration.getString("components.path").toSeq)
    out.reload
    out
  }

  def controllers: Seq[Controller] = new PocIntegrationImpl(componentLoader.all).controllers

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
    InternalServerError(org.corespring.web.common.views.html.onError(uid, throwable))
  }

  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Result = {
    val result = super.onHandlerNotFound(request)

    result match {
      case s: SimpleResult[_] => s.withHeaders(Headers.AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): play.api.mvc.Result = {
    val result = super.onBadRequest(request, error)
    result match {
      case s: SimpleResult[_] => s.withHeaders(Headers.AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onStart(app: Application): Unit = {

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

