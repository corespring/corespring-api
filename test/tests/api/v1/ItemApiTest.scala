package tests.api.v1

import org.junit.Ignore
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import api.ApiError._
import scala.Some
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import tests.BaseTest
import models.Item
import play.api.test.FakeHeaders
import play.api.libs.json.JsString
import scala.Some
import play.api.mvc.AnyContentAsJson

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
    items.size must beEqualTo( 50 )
  }

  "list items in a collection" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo( 50 )
  }

  "list all items skipping 30" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=30".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    (items(0) \ "id").as[String] must beEqualTo("4ffef41ce4b0cf00dc0a5024")
  }

  "list items limiting result to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo( 10 )
  }

  "find items in the grade level 7" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&q={\"gradeLevel\":\"07\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo( 17 )
  }

  "find items in returning only their title and up to 10" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=10".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.foreach(i => {
      (i \ "title").as[Option[String]] must beSome
      (i \ "author").as[Option[String]] must beNone
    })
    items.size must beEqualTo( 10  )
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
    val toCreate = xmlBody("<html></html>")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(BAD_REQUEST)
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "code").as[Int] must equalTo(CollectionIsRequired.code)
  }

  "create requires an authorized collection id" in {
    val toCreate = xmlBody("<html></html>", Map("collectionId" -> "something"))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(FORBIDDEN)
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "code").as[Int] must equalTo(CollectionUnauthorized.code)
  }

  "create response does not include csFeedbackIds" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = (Json.parse(contentAsString(result)) \ Item.data).toString
    xmlData must not(beMatching(".*csFeedbackId.*"))
  }

  "create does not accept id" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").toString

    val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("id" -> itemId))
    fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toUpdate))
    result = routeAndCall(fakeRequest).get
    var collection = Json.parse(contentAsString(result))
    (collection \ "code").as[Int] must equalTo(IdNotNeeded.code)
  }

  "update does not accept collection id" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]

    val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    fakeRequest = FakeRequest(PUT, "/api/v1/items/%s?access_token=%s".format(itemId, token), FakeHeaders(), AnyContentAsJson(toUpdate))
    result = routeAndCall(fakeRequest).get
    val collection = Json.parse(contentAsString(result))
    (collection \ "code").as[Int] must equalTo(CollIdNotNeeded.code)
  }

  "update does not include csFeedbackIds" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]

    val toUpdate = Map(Item.data -> "<html><feedbackInline></feedbackInline></html>")
    fakeRequest = FakeRequest(PUT, "/api/v1/items/%s?access_token=%s".format(itemId, token), FakeHeaders(), AnyContentAsJson(Json.toJson(toUpdate)))
    result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)


    val xmlFileContents: Seq[String] = getXMLContentFromResponse(contentAsString(result))
    xmlFileContents.foreach(_ must not(beMatching(".*csFeedbackId.*")))
  }

  "get item data with feedback contains csFeedbackIds" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> "5001bb0ee4b0d7c9ec3210a2"))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)

    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]
    val path: String = "/api/v1/items/%s?access_token=%s".format(itemId, token)
    val anotherFakeRequest = FakeRequest(GET, path)
    result = routeAndCall(anotherFakeRequest).get
    status(result) must equalTo(OK)

    val xmlFileContents: Seq[String] = getXMLContentFromResponse(contentAsString(result))
    xmlFileContents.foreach(_ must beMatching(".*csFeedbackId.*"))
  }

  /**
   * Generates JSON request body for the API, with provided XML data in the appropriate field. Also adds in a set of
   * top-level attributes that get added to the request.
   */
  private def xmlBody(xml: String, attributes: Map[String, String] = Map()): JsValue = {
    Json.toJson(
      attributes.iterator.foldLeft(
        Map(
        Item.data -> Json.toJson(
          Map(
            "name" -> JsString("qtiItem"),
            "files" -> Json.toJson(
              Seq(
                Json.toJson(
                  Map(
                    "name" -> Json.toJson("xml"),
                    "default" -> Json.toJson(false),
                    "contentType" -> Json.toJson("text/xml"),
                    "content" -> Json.toJson(xml)
                  )
                )
              )
            )
          )
        )
      ))((map, entry) => map + ((entry._1, Json.toJson(entry._2))))
    )
  }

  private def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => { (file \ "content").toString })
  }

}
