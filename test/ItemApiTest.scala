import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some

/**
 *
 */
class ItemApiTest extends BaseTest {
  "list all items" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 50
  }

  "list items in a collection" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 50
  }

  "list all items skipping 30" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=30".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    (items(0) \ "id").as[String] must beEqualTo("50086e0ae4b03c53174f7456")
  }

  "list items limiting result to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 10
  }

  "find items in the Science category" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&q={\"gradeLevel\":\"07\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 11
  }

  "find items in returning only their title and up to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.foreach( i => {
      (i \ "title").as[Option[String]] must beSome
      (i \ "author").as[Option[String]] must beNone
    })
    items must have size 10
  }

  "get an item by id" in {
    val id = "50085085e4b081257be515f4"
    val fakeRequest = FakeRequest(GET, "/api/v1/items/%s?access_token=%s".format(id, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    Logger.info("charset = " + charset(result))
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val item = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (item \ "id").as[String] must beEqualTo(id)
  }

  "create update and delete an item" in {

  }


}
