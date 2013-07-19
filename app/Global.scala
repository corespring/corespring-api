import _root_.controllers.ConcreteS3Service
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.typesafe.config.ConfigFactory
import common.controllers.deployment.{LocalAssetsLoaderImpl, AssetsLoaderImpl}
import common.seed.SeedDb._
import org.bson.types.ObjectId
import play.api._
import play.api.mvc.Results._
import play.api.mvc._
import scala.Some
import web.controllers.utils.ConfigLoader


/**
  */
object Global extends GlobalSettings {

  val INIT_DATA: String = "INIT_DATA"

  val AccessControlAllowEverything = ("Access-Control-Allow-Origin", "*")

  def AccessControlAction[A](action: Action[A]): Action[A] = Action(action.parser) {
    request =>
      action(request) match {
        case s: SimpleResult[_] =>
          s
            .withHeaders(AccessControlAllowEverything)
            .withHeaders(("Access-Control-Allow-Methods", "PUT, GET, POST, DELETE, OPTIONS"))
            .withHeaders(("Access-Control-Allow-Headers", "x-requested-with,Content-Type,Authorization"))

        case result => result
      }
  }

  def AjaxFilterAction[A](action: Action[A]): Action[A] = Action(action.parser) {
    request =>
      if (request.headers.get("X-Requested-With") == Some("XMLHttpRequest")) {
        action(request) match {
          case s: SimpleResult[_] => s.withHeaders(("Cache-Control", "no-cache"))
          case result => result
        }
      }
      else {
        action(request)
      }
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(AccessControlAction(Action(new play.api.mvc.Results.Status(200))))
      case _ => {
        super.onRouteRequest(request).map {
          case action: Action[_] => AjaxFilterAction(AccessControlAction(action))
          case other => other
        }
      }
    }
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {

    val uid = new ObjectId().toString
    Logger.error(uid)
    Logger.error(throwable.getMessage)

    if (Logger.isDebugEnabled) {
      throwable.printStackTrace()
    }
    InternalServerError(common.views.html.onError(uid, throwable))
  }


  override def onHandlerNotFound(request: play.api.mvc.RequestHeader): Result = {
    val result = super.onHandlerNotFound(request)

    result match {
      case s: SimpleResult[_] => s.withHeaders(AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onBadRequest(request: play.api.mvc.RequestHeader, error: scala.Predef.String): play.api.mvc.Result = {
    val result = super.onBadRequest(request, error)
    result match {
      case s: SimpleResult[_] => s.withHeaders(AccessControlAllowEverything)
      case _ => result
    }
  }

  override def onStart(app: Application) : Unit = {
    // support JodaTime
    RegisterJodaTimeConversionHelpers()

    ConcreteS3Service.init
    AssetsLoaderImpl.init(app)
    LocalAssetsLoaderImpl.init(app)

    val initData:Boolean = ConfigFactory.load().getString(INIT_DATA) == "true"

    def onlyIfLocalDb(fns: (() => Unit)*) {
      if (isLocalDb)
        fns.foreach( fn => fn() )
      else
        throw new RuntimeException("You're trying to seed against a remote db - bad idea")
    }

    app.mode match {
      case Mode.Test => {
        emptyData()
        seedTestData()
      }
      case Mode.Dev => {
        if(initData) {
          onlyIfLocalDb(emptyData, seedDevData, seedDebugData)
        }
        seedStaticData()
      }
      case Mode.Prod => {
        if(initData){
          emptyData()
          seedDevData()
        }
        seedStaticData()
        seedDemoData()
      }
    }

  }

  private def isLocalDb: Boolean = {
    ConfigLoader.get("mongodb.default.uri") match {
      //TODO: Remove hardcoded url
      case Some(url) => (url.contains("localhost") || url.contains("127.0.0.1") || url == "mongodb://bleezmo:Basic333@ds035907.mongolab.com:35907/sib")
      case None => false
    }
  }

  /** Add demo data models to the the db to allow end users to be able to
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

  private def seedTestData() {
    seedData("conf/seed-data/test")
  }

  private def seedDevData() {
    seedData("conf/seed-data/common")
    seedData("conf/seed-data/dev")
    seedData("conf/seed-data/exemplar-content")
  }

  private def seedDebugData(){
    //do not call emptyData() as it expects to be called after seedDevData
    seedData("conf/seed-data/debug")
  }

}


