import _root_.controllers.S3Service
import com.ee.seeder.MongoDbSeeder
import _root_.models.auth.AccessToken
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api._
import mvc._
import mvc.SimpleResult
import play.api.Play.current
import play.api.Application
import web.controllers.utils.ConfigLoader

/**
  */
object Global extends GlobalSettings {

  val AUTO_RESTART: String = "AUTO_RESTART"
  val INIT_DATA: String = "INIT_DATA"

  val MOCK_ACCESS_TOKEN: String = "34dj45a769j4e1c0h4wb"

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

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {

    request.method match {
      //return the default access control headers for all OPTION requests.
      case "OPTIONS" => Some(AccessControlAction(Action(new play.api.mvc.Results.Status(200))))
      case _ => {
        super.onRouteRequest(request).map {
          case action: Action[_] => AccessControlAction(action)
          case other => other
        }
      }
    }
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
    val amazonProperties = Play.getFile("/conf/AwsCredentials.properties")
    S3Service.init(amazonProperties)

    val initData = ConfigLoader.get(INIT_DATA).getOrElse("true") == "true"

    val mongoUri = ConfigLoader.get("mongodb.default.uri")

    if (Play.isTest(app) && mongoUri.isDefined) {
      seedTestData(mongoUri.get)
    } else {
      if (initData && mongoUri.isDefined) seedDevData(mongoUri.get)
    }
  }


  private def seedTestData(uri:String)  {

    val folders = List(
      "conf/seed-data/common",
      "conf/seed-data/test"
    )

    Logger.info("reseed: \n" + folders.mkString("\n"))
    MongoDbSeeder.emptyDb(uri, folders)
    MongoDbSeeder.seed(uri, folders)
    addMockAccessToken(MOCK_ACCESS_TOKEN)
  }

  private def seedDevData(uri:String)  {

    val folders = List(
      "conf/seed-data/common",
      "conf/seed-data/dev",
      "conf/seed-data/exemplar-content"
    )
    Logger.info("reseed: \n" + folders.mkString("\n"))
    MongoDbSeeder.emptyDb(uri, folders)
    MongoDbSeeder.seed(uri, folders)
    addMockAccessToken(MOCK_ACCESS_TOKEN)
  }

  def addMockAccessToken(token: String) = {
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val accessToken = AccessToken(new ObjectId("502404dd0364dc35bb393397"), None, token, creationDate, creationDate.plusHours(24))
    AccessToken.insert(accessToken)
  }

}


