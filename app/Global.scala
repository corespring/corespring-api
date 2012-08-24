import _root_.controllers.auth.Permission
import _root_.controllers.{S3Service, Log}
import _root_.models.auth.{AccessToken, ApiClient}
import _root_.models._
import _root_.models.Content
import akka.util.Duration
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.util.JSON
import java.io.File
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import java.util.{TimerTask, Timer}
import org.bson.types.ObjectId
import org.joda.time.DateTime
import play.api._
import http.Status
import libs.concurrent.Akka
import libs.iteratee.Enumerator
import mvc._
import mvc.SimpleResult
import scala.io.Codec
import com.mongodb.casbah.Imports._
import play.api.Play.current
import com.novus.salat._
import com.novus.salat.global._
import play.api.Application

/**
 */
object Global extends GlobalSettings {

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
    if (Play.isDev(app) || Play.isTest(app) || System.getenv("AUTO_RESTART") == "true") {
      insertTestData("/conf/test-data/")
    }
    if(System.getenv("AUTO_RESTART") == "true"){
      Akka.system.scheduler.scheduleOnce(Duration.create(1, TimeUnit.DAYS)){
      Play.start(new Application(Play.current.path,Play.current.classloader,Play.current.sources,Play.current.mode))
      }
    }
  }

  private def insertTestData(basePath: String) = {
    def jsonLinesToDb(jsonPath: String, coll: MongoCollection) = {
      coll.drop()
      val lines: Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).getLines()
      for (line <- lines) {
        insertString(line, coll)
      }
    }

    def jsonFileToDb(jsonPath: String, coll: MongoCollection) {
      coll.drop()
      val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.defaultCharset())).mkString
      insertString(s, coll)
    }

    def insertString(s: String, coll: MongoCollection) = coll.insert(JSON.parse(s).asInstanceOf[DBObject], coll.writeConcern)

    jsonLinesToDb(basePath + "orgs.json", Organization.collection)
    jsonLinesToDb(basePath + "items.json", Content.collection)
    jsonLinesToDb(basePath + "collections.json", ContentCollection.collection)
    jsonLinesToDb(basePath + "apiClients.json", ApiClient.collection)
    jsonLinesToDb(basePath + "users.json", User.collection)
    jsonLinesToDb(basePath + "itemsessions.json", ItemSession.collection)
    jsonLinesToDb(basePath + "subjects.json", Subject.collection)
    jsonLinesToDb(basePath + "standards.json", Standard.collection)

    jsonFileToDb(basePath + "fieldValues.json", FieldValue.collection)
    //acces token stuff
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val token = AccessToken(new ObjectId("502404dd0364dc35bb393397"), None, "34dj45a769j4e1c0h4wb", creationDate, creationDate.plusHours(24))
    AccessToken.insert(token)
  }

}
