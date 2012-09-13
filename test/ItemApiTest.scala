import org.junit.Ignore
import play.api.libs.json.{JsValue, Json}
import play.api.Logger
import play.api.mvc.{Result, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import api.ApiError._

/**
 *
 */
class ItemApiTest extends BaseTest {
  "list all items" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 50
  }

  "list items in a collection" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 50
  }

  "list all items skipping 30" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=30".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    (items(0) \ "id").as[String] must beEqualTo("5006cbb3e4b0df23296000da")
  }

  "list items limiting result to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 10
  }

  "find items in the grade level 7" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&q={\"gradeLevel\":\"07\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items must have size 14
  }

  "find items in returning only their title and up to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
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
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val item = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (item \ "id").as[String] must beEqualTo(id)
  }

  "create requires a collection id" in {
    val toCreate = Map("xmlData" -> "<html></html>")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(BAD_REQUEST)
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "code").as[Int] must equalTo(CollectionIsRequired.code)
  }

  "create requires an authorized collection id" in {
    val toCreate = Map("xmlData" -> "<html></html>", "collectionId" -> "something")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(FORBIDDEN)
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "code").as[Int] must equalTo(CollectionUnauthorized.code)
  }

  "create response does not include csFeedbackIds" in {
    val toCreate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = (Json.parse(contentAsString(result)) \ "xmlData").toString
    xmlData must not (beMatching(".*csFeedbackId.*"))
  }

  "create does not accept id" in {
    val toCreate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").toString

    val toUpdate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "id" -> itemId)
    fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    result = routeAndCall(fakeRequest).get
    var collection = Json.parse(contentAsString(result))
    (collection \ "code").as[Int] must equalTo(IdNotNeeded.code)
  }

  "update does not accept collection id" in {
    val toCreate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]

    val toUpdate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    fakeRequest = FakeRequest(PUT, "/api/v1/items/%s?access_token=%s".format(itemId,  token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    result = routeAndCall(fakeRequest).get
    val collection = Json.parse(contentAsString(result))
    (collection \ "code").as[Int] must equalTo(CollIdNotNeeded.code)
  }

  "update does not include csFeedbackIds" in {
    val toCreate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]

    val toUpdate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>")
    fakeRequest = FakeRequest(PUT, "/api/v1/items/%s?access_token=%s".format(itemId,  token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = (Json.parse(contentAsString(result)) \ "xmlData").toString
    xmlData must not (beMatching(".*csFeedbackId.*"))
  }


  "get item data with feedback contains csFeedbackIds" in {
    val toCreate = Map("xmlData" -> "<html><feedbackInline></feedbackInline></html>", "collectionId" -> "5001bb0ee4b0d7c9ec3210a2")
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(Json.toJson(toCreate)))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]
    val path: String = "/api/v1/items/%s/data?access_token=%s".format(itemId, token)


    val anotherFakeRequest = FakeRequest(GET, path)
    result = routeAndCall(anotherFakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = contentAsString(result).toString
    xmlData must beMatching(".*csFeedbackId.*")
  }
}
