package org.corespring.test

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.{ItemService, ItemServiceImpl}
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mutable.Specification
import play.api.libs.json._
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }
import scala.Some
import scala.concurrent.Future

trait BaseTest extends Specification {

  val TEST_COLLECTION_ID: String = "51114b127fc1eaa866444647"
  // From standard fixture data
  val token = "test_token"

  def itemService: ItemService = ItemServiceImpl

  def fakeRequest(content: AnyContent = AnyContentAsEmpty): FakeRequest[AnyContent] = FakeRequest("", tokenize(""), FakeHeaders(), content)

  PlaySingleton.start()

  /**
   * Decorate play.api.mvc.Result with some helper methods
   */
  class ResultHelper(result: Future[SimpleResult]) {
    def \(key: String): String = (Json.parse(contentAsString(result)) \ key).as[String]

    def body: String = contentAsString(result).toString
  }

  implicit def resultToResultHelper(result: Future[SimpleResult]) = new ResultHelper(result)

  def tokenize(url: String, tkn: String = token): String = if(url.contains("?")) url+"&access_token="+tkn else url + "?access_token=" + tkn

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

  def versionedId(oid: String, v: Int = 0): VersionedId[ObjectId] = VersionedId(new ObjectId(oid), Some(v))

  // TODO: Something's wrong with this, but when it works it will be a useful shorthand
  def doRequest(httpVerb: String, url: String, jsonObject: Map[String, String]): Option[Future[SimpleResult]] =
    doRequest(httpVerb, url, Some(jsonObject))

  def doRequest(httpVerb: String, url: String, jsonObject: Option[Map[String, String]] = None): Option[Future[SimpleResult]] = {
    import play.api.test.Helpers._
    require(Set(GET, PUT, POST, DELETE).contains(httpVerb))

    val fullUrl = url + "?access_token=%s".format(token)

    val request = jsonObject match {
      case Some(jsonMap) =>
        FakeRequest(httpVerb, fullUrl, FakeHeaders(), AnyContentAsJson(Json.toJson(jsonMap)))
      case None =>
        FakeRequest(httpVerb, fullUrl)
    }
    //TODO 2.1.3 - using route gives a compilation error here
    routeAndCall(request)
  }

  /**
   * Generates JSON request body for the API, with provided XML data in the appropriate field. Also adds in a set of
   * top-level attributes that get added to the request.
   */
  def xmlBody(xml: String, attributes: Map[String, String] = Map()): JsValue = Json.toJson(attributes)

  def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.Keys.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => {
      (file \ "content").toString
    })
  }

  def parsed[A](result: Future[SimpleResult])(implicit reads: Reads[A]) = Json.fromJson[A](Json.parse(contentAsString(result))) match {
    case JsSuccess(data, _) => data
    case _ => throw new RuntimeException("Couldn't parse json")
  }

  def assertResult(result: Future[SimpleResult],
    expectedStatus: Int = OK,
    expectedCharset: Option[String] = Some("utf-8"),
    expectedContentType: Option[String] = Some("application/json")): org.specs2.execute.Result = {
    status(result) === expectedStatus
    charset(result) === expectedCharset
    contentType(result) === expectedContentType
  }

}
