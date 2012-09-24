package tests.api.v1

import org.junit.Ignore
import play.api.libs.json._
import play.api.Logger
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import api.ApiError._
import scala.Some
import play.api.test.FakeHeaders
import tests.BaseTest
import models.{VirtualFile, Resource, Item}
import play.api.test.FakeHeaders
import play.api.libs.json.JsString
import scala.Some
import org.bson.types.ObjectId
import controllers.Log
import play.api.test.FakeHeaders
import play.api.libs.json.JsString
import scala.Some
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import scala.xml._

class ItemApiTest extends BaseTest {

  val TEST_COLLECTION_ID : String = "5001bb0ee4b0d7c9ec3210a2"

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
    // TODO - this test works when run with test-only but fails in suite, presumably because another test is making mock data
    pending
    //(items(0) \ "id").as[String] must beEqualTo("4ffef41ce4b0cf00dc0a5024")
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
    items.size must beEqualTo( 19 )
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
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = (Json.parse(contentAsString(result)) \ Item.data).toString
    xmlData must not(beMatching(".*csFeedbackId.*"))
  }

  "create does not accept id" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
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
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (Json.parse(contentAsString(result)) \ "id").as[String]

    val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    fakeRequest = FakeRequest(PUT, "/api/v1/items/%s?access_token=%s".format(itemId, token), FakeHeaders(), AnyContentAsJson(toUpdate))
    result = routeAndCall(fakeRequest).get
    val collection = Json.parse(contentAsString(result))
    (collection \ "code").as[Int] must equalTo(CollIdNotNeeded.code)
  }

  "update with no collectionId returns the item's stored collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = api.v1.routes.ItemApi.createItem()
    val createResult = routeAndCall( FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)
    val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.author -> "Ed"))

    val updateCall = api.v1.routes.ItemApi.updateItem(new ObjectId(id))

    val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    Log.i(contentAsString(updateResult))
    val item : Item = Json.parse(contentAsString(updateResult)).as[Item]

    item.collectionId must equalTo(TEST_COLLECTION_ID)
  }

  val STATE_DEPT : String = "State Department of Education"

  "get and update return the same json" in {

    val toCreate = xmlBody("<root/>", Map(Item.collectionId -> TEST_COLLECTION_ID,Item.credentials -> STATE_DEPT))
    val call = api.v1.routes.ItemApi.createItem()
    val createResult = routeAndCall( FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]

    val getItemCall = api.v1.routes.ItemApi.getItem(new ObjectId(id))
    val getResult = routeAndCall( FakeRequest(getItemCall.method, tokenize(getItemCall.url), FakeHeaders(), AnyContentAsEmpty)).get

    val getJsonString = contentAsString(getResult)

    val updateCall = api.v1.routes.ItemApi.updateItem(new ObjectId(id))

    val toUpdate = xmlBody("<root/>", Map(Item.credentials -> STATE_DEPT ))
    val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    val updateJsonString = contentAsString(updateResult)
    updateJsonString must equalTo(getJsonString)
  }



  "when saving an item with QTI xml, add csFeedbackId attrs if they are not present" in {
    /**
     * all feedback elements, feedbackInline and modalFeedback should be decorated with the attribute csFeedbackId
     * on persist/update of an item if the attribute is not already there. These need to be unique within the item, so itemId-n would work as id
     *
     * NOTE: test data loaded to db may be missing this if it is loaded statically. Could load a the test composite item
     * (50083ba9e4b071cb5ef79101) and save it. Other tests might fail if these csFeedbackId attrs are not present
     */
    var jsitem = Json.toJson(Item.findOneById(new ObjectId("50083ba9e4b071cb5ef79101"))).asInstanceOf[JsObject]
    jsitem = JsObject(jsitem.fields.filter(field => field._1 != "id" && field._1 != "collectionId"))
    var fakeRequest = FakeRequest(PUT, "/api/v1/items/50083ba9e4b071cb5ef79101?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(jsitem))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val resource:Resource = Item.findOneById(new ObjectId("50083ba9e4b071cb5ef79101")).get.data.get
    val qtiXml:Option[String] = resource.files.find(file => file.isMain).map(file => file.asInstanceOf[VirtualFile].content)
    qtiXml must beSome[String]
    val hasCsFeedbackIds:Boolean = qtiXml.map(qti =>
      findFeedbackIds(XML.loadString(qti),Seq(),0).find(result => {
        if(result._2) false else true
      }).isEmpty
    ).getOrElse(false)
    hasCsFeedbackIds must beTrue
  }

  private def findFeedbackIds(xml:Elem, acc:Seq[(NodeSeq,Boolean)], levels: Int): Seq[(NodeSeq,Boolean)] = {
    var feedback:Seq[(NodeSeq,Boolean)] = acc
    val children = xml.child
    for (child <- children){
      val feedbackInline = child \ "feedbackInline"
      if (feedbackInline.isEmpty) {
        child match {
          case innerXml:Elem => feedback = findFeedbackIds(innerXml,feedback,levels+1)
          case _ =>
        }
      }
      else {
        val feedbackInlines = feedbackInline.theSeq
        for (feedbackNode <- feedbackInlines) {
          if ((feedbackNode \ "@csFeedbackId").nonEmpty) {
            feedback = feedback :+ (feedbackInline -> true)
          }
          else {
            feedback = feedback :+ (feedbackInline -> false)
          }
        }
      }
    }
    feedback
  }


}
