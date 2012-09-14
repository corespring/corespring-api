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
import libs.json.{Json, JsObject}
import mvc._
import mvc.SimpleResult
import scala.io.Codec
import com.mongodb.casbah.Imports._
import play.api.Play.current
import com.novus.salat._
import com.novus.salat.global._
import dao.SalatInsertError
import play.api.Application
import _root_.web.config.InitialData
import scala.util.matching.Regex
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
    if (Play.isTest(app) || initData) {
      insertTestData("/conf/test-data/")
    }

    InitialData.insert()
  }

  private def insertTestData(basePath: String) = {
    def jsonLinesToDb(jsonPath: String, coll: MongoCollection) = {
      coll.drop()
      val lines: Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).getLines()
      for (line <- lines) {
        insertString(line, coll)
      }
    }

    def jsonFileToDb(jsonPath: String, coll: MongoCollection, drop: Boolean = true) {


      if (drop) coll.drop()

      val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.defaultCharset())).mkString
      coll.insert(JSON.parse(s).asInstanceOf[DBObject])
    }


    def interpolate(text: String, lookup: String => String) =
      """\$\[interpolate\{([^}]+)\}\]""".r.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
        case Regex.Groups(v) => {

          val result = lookup(v)
          result
        }
      })

    def replaceLinksWithContent(s: String): String = {


      /**
       * Load a string from a given path, remove new lines and escape "
       * @param path
       * @return
       */
      def loadString(path: String): String = {
        val s = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.defaultCharset())).mkString


        val lines = s.replaceAll("\n", "\\\\\n")
        //TODO: I had "\\\\\"" here as the replacement but it didn't work.
        val quotes = lines.replaceAll("\"", "'")
        quotes
      }
      val interpolated = interpolate(s, loadString)
      interpolated
    }

    def jsonFileToItem(jsonPath: String, coll: MongoCollection, drop: Boolean = true, xmlPath: String = null) {
      if (drop) {
        coll.drop()
      }


      val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.defaultCharset())).mkString
      val finalObject: String = replaceLinksWithContent(s)

      /**
       * Force the collection id
       * TODO: Speak with others about setting this up correctly.
       */
      val FORCED_COLLECTION_ID = "5001b9b9e4b035d491c268c3"
      val forcedCollectionId = finalObject.replaceAll("\"collectionId\".*?:.*?\".*?\",", "\"collectionId\" : \""+FORCED_COLLECTION_ID+"\",")
      insertString(forcedCollectionId, coll)

    }
    def insertString(s: String, coll: MongoCollection) = coll.insert(JSON.parse(s).asInstanceOf[DBObject], coll.writeConcern)

    jsonFileToDb(basePath + "fieldValues.json", FieldValue.collection)
    jsonLinesToDb(basePath + "orgs.json", Organization.collection)


    Content.collection.drop()
    if (Play.isTest) {
      jsonLinesToDb(basePath + "items.json", Content.collection)
    }
    jsonFileToItem(basePath + "item-with-supporting-materials.json", Content.collection, drop = false, xmlPath = "/conf/qti/composite-with-feedback.xml")
    jsonFileToItem(basePath + "item-with-html-test.json", Content.collection, drop = false)

    val ExemplarContent = "exemplar-content"

    //load examplar content
    val folder : File = Play.getFile(basePath + ExemplarContent)
    for (file <- folder.listFiles) {
      Logger.info("adding: " + file.getName)
      jsonFileToItem(basePath + ExemplarContent + "/" + file.getName, Content.collection, drop = false)
    }

    jsonLinesToDb(basePath + "collections.json", ContentCollection.collection)
    jsonLinesToDb(basePath + "apiClients.json", ApiClient.collection)
    jsonLinesToDb(basePath + "users.json", User.collection)
    jsonLinesToDb(basePath + "itemsessions.json", ItemSession.collection)
    jsonLinesToDb(basePath + "subjects.json", Subject.collection)
    jsonLinesToDb(basePath + "standards.json", Standard.collection)
    Logger.info("insert item with supporting materials")
    jsonFileToItem(basePath + "item-with-supporting-materials.json", Content.collection, drop = false, xmlPath = "/conf/qti/single-choice.xml")
    jsonFileToItem(basePath + "item-with-html-test.json", Content.collection, drop = false  )

    //acces token stuff
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val token = AccessToken(new ObjectId("502404dd0364dc35bb393397"), None, MOCK_ACCESS_TOKEN, creationDate, creationDate.plusHours(24))
    AccessToken.insert(token)
  }

}
