import _root_.controllers.auth.Permission
import _root_.controllers.Log
import _root_.models.auth.{AccessToken, ApiClient}
import _root_.models._
import _root_.models.Content
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.util.JSON
import java.nio.charset.Charset
import org.bson.types.ObjectId
import play.api._
import http.Status
import libs.iteratee.Enumerator
import mvc._
import mvc.SimpleResult
import scala.io.Codec
import com.mongodb.casbah.Imports._
import play.api.Play.current
import com.novus.salat._
import com.novus.salat.global._

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
    if (Play.isDev(app) || Play.isTest(app)) {
      insertTestData("/conf/test-data/")
    }
  }

  private def insertTestData(basePath: String) = {
    def jsonToDB(jsonPath: String, coll: MongoCollection) = {
      coll.drop()
      val lines: Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.defaultCharset())).getLines()
      for (line <- lines) {
        coll.insert(JSON.parse(line).asInstanceOf[DBObject], coll.writeConcern)
      }
    }
    jsonToDB(basePath + "orgs.json", Organization.collection)
    jsonToDB(basePath + "items.json", Content.collection)
    jsonToDB(basePath + "collections.json", ContentCollection.collection)
    jsonToDB(basePath + "apiClients.json", ApiClient.collection)
    jsonToDB(basePath + "accessTokens.json", AccessToken.collection)
    jsonToDB(basePath + "users.json", User.collection)
    jsonToDB(basePath + "itemsessions.json", ItemSession.collection)
  }

}
