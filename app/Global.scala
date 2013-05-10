import _root_.controllers.ConcreteS3Service
import _root_.models.itemSession.{ArrayItemResponse, StringItemResponse, ItemSession}
import _root_.models.quiz.basic.{Participant, Answer, Quiz}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import play.api.mvc.Results._
import util.Random
import web.controllers.utils.ConfigLoader
import common.seed.SeedDb._
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.bson.types.ObjectId
import play.api._
import mvc._
import mvc.SimpleResult
import play.api.Play.current
import play.api.Application


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

  override def onStart(app: Application) {

    // support JodaTime
    RegisterJodaTimeConversionHelpers()

    ConcreteS3Service.init

    val initData:Boolean = ConfigFactory.load().getString(INIT_DATA) == "true"

    def onlyIfLocalDb(fn: (() => Unit)) {
      if (isLocalDb)
        fn()
      else
        throw new RuntimeException("You're trying to seed against a remote db - bad idea")
    }

    if(!Play.isTest(app)) {
      if (Play.isDev(app) && initData) {
        onlyIfLocalDb(seedDevData)
      } else if(Play.isProd(app) && initData) {
        seedDevData()
      }
      addDemoDataToDb()
    } else {
      seedTestData()
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
  private def addDemoDataToDb() {
    seedData("conf/seed-data/demo")
  }

  private def isLocalDb: Boolean = {
    ConfigLoader.get("mongodb.default.uri") match {
      case Some(url) => (url.contains("localhost") || url.contains("127.0.0.1") || url == "mongodb://bleezmo:Basic333@ds035907.mongolab.com:35907/sib")
      case None => false
    }
  }

  private def seedTestData() {
    emptyData()
    seedData("conf/seed-data/test")
  }

  private def seedDevData() {
    emptyData()
    seedData("conf/seed-data/common")
    seedData("conf/seed-data/dev")
    seedData("conf/seed-data/exemplar-content")
  }

}


