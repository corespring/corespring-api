package tests

import _root_.common.seed.SeedDb
import _root_.models.item.Item
import _root_.models.item.service.{ItemService, ItemServiceImpl}
import _root_.web.controllers.utils.ConfigLoader
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.AnyContentAsJson
import play.api.mvc.AnyContentAsText
import play.api.libs.json.JsObject
import org.corespring.platform.data.mongo.models.VersionedId


/**
 * Base class for tests
 *
 */
trait BaseTest extends Specification {

  // From standard fixture data
  val token = "test_token"

  val isLocalDb: Boolean = {
    ConfigLoader.get("mongodb.default.uri") match {
      case Some(url) => (url.contains("localhost") || url.contains("127.0.0.1") || url == "mongodb://bleezmo:Basic333@ds035907.mongolab.com:35907/sib")
      case None => false
    }
  }

  def itemService : ItemService = ItemServiceImpl

  def fakeRequest(content:AnyContent = AnyContentAsEmpty) : FakeRequest[AnyContent] = FakeRequest("", tokenize(""), FakeHeaders(), content)

  def initDB() {
    if (isLocalDb) {
      SeedDb.emptyData()
      SeedDb.seedData("conf/seed-data/test")
    } else {
      throw new RuntimeException("You're trying to seed against a remote db - bad idea")
    }
  }

  PlaySingleton.start()

  override def map(fs: => Fragments) = Step(initDB) ^ fs

  /**
   * Decorate play.api.mvc.Result with some helper methods
   */
  class ResultHelper(result: Result) {
    def \(key: String): String = (Json.parse(contentAsString(result)) \ key).as[String]

    def body: String = contentAsString(result).toString
  }

  implicit def resultToResultHelper(result: Result) = new ResultHelper(result)

  def tokenize(url: String, tkn: String = token): String = url + "?access_token=" + tkn

  def tokenFakeRequest[A](method: String, uri: String, headers: FakeHeaders = FakeHeaders(), body: A = AnyContentAsText("")): FakeRequest[A] = {
    FakeRequest(method, tokenize(uri), headers, body)
  }

  /** When passing a request to an Action you don't need the url or method */
  def tokenFakeRequest[A](headers: FakeHeaders = FakeHeaders(), body: A = AnyContentAsText("")): FakeRequest[A] = {
    FakeRequest("blah", tokenize("blah"), headers, body)
  }

  /**
   * @param id item id
   * @return
   */
  def item(id: String): Item = {

    ItemServiceImpl.findOneById(VersionedId(new ObjectId(id))) match {
      case Some(item) => {
        item
      }
      case _ => throw new RuntimeException("test item")
    }
  }

  def versionedId(oid: String, v : Int = 0): VersionedId[ObjectId] = VersionedId(new ObjectId(oid), Some(v))


  // TODO: Something's wrong with this, but when it works it will be a useful shorthand
  def doRequest(httpVerb: String, url: String, jsonObject: Map[String, String]): Option[Result] =
    doRequest(httpVerb, url, Some(jsonObject))

  def doRequest(httpVerb: String, url: String, jsonObject: Option[Map[String, String]] = None): Option[Result] = {
    require(Set(GET, PUT, POST, DELETE).contains(httpVerb))

    val fullUrl = url + "?access_token=%s".format(token)

    val request = jsonObject match {
      case Some(jsonMap) =>
        FakeRequest(httpVerb, fullUrl, FakeHeaders(), AnyContentAsJson(Json.toJson(jsonMap)))
      case None =>
        FakeRequest(httpVerb, fullUrl)
    }
    //TODO 2.1.1 - using route gives a compilation error here
    routeAndCall(request)
  }

  /**
   * Generates JSON request body for the API, with provided XML data in the appropriate field. Also adds in a set of
   * top-level attributes that get added to the request.
   */
  def xmlBody(xml: String, attributes: Map[String, String] = Map()): JsValue =  Json.toJson(attributes)

  def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.Keys.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => {
      (file \ "content").toString
    })
  }


  def parsed[A](result: Result)(implicit reads: Reads[A]) = Json.fromJson[A](Json.parse(contentAsString(result))) match {
    case JsSuccess(data, _) => data
    case _ => throw new RuntimeException("Couldn't parse json")
  }

  def assertResult(result: Result,
                   expectedStatus: Int = OK,
                   expectedCharset: Option[String] = Some("utf-8"),
                   expectedContentType: Option[String] = Some("application/json")): org.specs2.execute.Result = {
    status(result) === expectedStatus
    charset(result) === expectedCharset
    contentType(result) === expectedContentType
  }

}
