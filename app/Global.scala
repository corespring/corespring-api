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

    import JsonImporter._

    jsonFileToDb(basePath + "fieldValues.json", FieldValue.collection)
    jsonLinesToDb(basePath + "orgs.json", Organization.collection)


    Content.collection.drop()
    jsonLinesToDb(basePath + "items.json", Content.collection)
    jsonFileToItem(basePath + "item-with-supporting-materials.json", Content.collection, drop = false )
    jsonFileToItem(basePath + "item-with-html-test.json", Content.collection, drop = false)

    val ExemplarContent = "exemplar-content"

    //load examplar content
    /*
    val folder : File = Play.getFile(basePath + ExemplarContent)
    for (file <- folder.listFiles) {
      Logger.info("adding: " + file.getName)
      jsonFileToItem(basePath + ExemplarContent + "/" + file.getName, Content.collection, drop = false)
    }
     */
    jsonFileToItem(basePath + ExemplarContent + "/5020025fe4b0b0fa073aa3d7-The-shapes-of.json", Content.collection, drop = false)

    //Subjects and standards
    jsonFileListToDb(basePath + "subjects.json", Subject.collection)
    jsonFileListToDb(basePath + "standards.json", Standard.collection)

    jsonLinesToDb(basePath + "collections.json", ContentCollection.collection)
    jsonLinesToDb(basePath + "apiClients.json", ApiClient.collection)
    jsonLinesToDb(basePath + "users.json", User.collection)
    jsonLinesToDb(basePath + "itemsessions.json", ItemSession.collection)

    //acces token stuff
    AccessToken.collection.drop()
    val creationDate = DateTime.now()
    val token = AccessToken(new ObjectId("502404dd0364dc35bb393397"), None, MOCK_ACCESS_TOKEN, creationDate, creationDate.plusHours(24))
    AccessToken.insert(token)
  }

}

object StringUtils {

  def interpolate(text: String, lookup: String => String) =
    """\$\[interpolate\{([^}]+)\}\]""".r.replaceAllIn(text, (_: scala.util.matching.Regex.Match) match {
      case Regex.Groups(v) => {

        val result = lookup(v)
        result
      }
    })
}

object JsonImporter {


  /**
   * Insert each line of the file as a single object
   * @param jsonPath
   * @param coll
   */
  def jsonLinesToDb(jsonPath: String, coll: MongoCollection, drop : Boolean = true) {

    if(drop) coll.drop()

    val lines: Iterator[String] = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).getLines()
    for (line <- lines) {
      if (line != null && line != ""){
        insertString(line, coll)
      }
    }
  }

  /**
   * Read a complete file as a single json object
   * @param jsonPath
   * @param coll
   * @param drop
   */
  def jsonFileToDb(jsonPath: String, coll: MongoCollection, drop: Boolean = true) {
    if (drop) coll.drop()
    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    coll.insert(JSON.parse(s).asInstanceOf[DBObject])
  }

  def jsonFileToItem(jsonPath: String, coll: MongoCollection, drop: Boolean = true) {
    if (drop) {
      coll.drop()
    }

    val s = io.Source.fromFile(Play.getFile(jsonPath))(new Codec(Charset.forName("UTF-8"))).mkString
    val finalObject: String = replaceLinksWithContent(s)
    insertString(finalObject, coll)
  }

  /**
   * Insert a json file that is a json array of items into the db
   * @param path = path to json
   * @param coll
   */
  def jsonFileListToDb(path:String, coll:MongoCollection) {
    coll.drop()
    val listString = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.forName("UTF-8"))).mkString
    val dbList = com.mongodb.util.JSON.parse(listString).asInstanceOf[com.mongodb.BasicDBList]
    Logger.info("Adding " + dbList.size() + " to: " + coll.name )
    dbList.toList.foreach(  dbo => coll.insert(dbo.asInstanceOf[DBObject]))
  }

  /**
   * replace any interpolated keys with the path that they point to eg:
   * $[interpolate{/path/to/file.xml}] will be replaced with the contents of file.xml
   * @param s
   * @return
   */
  def replaceLinksWithContent(s: String): String = {

    /**
     * Load a string from a given path, remove new lines and escape "
     * @param path
     * @return
     */
    def loadString(path: String): String = {
      val s = io.Source.fromFile(Play.getFile(path))(new Codec(Charset.forName("UTF-8"))).mkString
      val lines = s.replaceAll("\n", "\\\\\n")
      //TODO: I had "\\\\\"" here as the replacement but it didn't work.
      val quotes = lines.replaceAll("\"", "'")
      quotes
    }
    val interpolated = StringUtils.interpolate(s, loadString)
    interpolated
  }

  def insertString(s: String, coll: MongoCollection) = {
    val dbo : DBObject = JSON.parse(s).asInstanceOf[DBObject]
    val id = dbo.get("_id").toString

    coll.findOneByID( new ObjectId( id )) match {
      case Some(obj) => throw new RuntimeException("Item already exisits: " + id + " collection: " + coll.name)
      case _ => coll.insert(dbo, coll.writeConcern)
    }
  }


}
