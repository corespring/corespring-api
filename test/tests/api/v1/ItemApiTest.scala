package tests.api.v1

import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import tests.BaseTest
import models._
import auth.AccessToken
import item.Item
import item.resource.{VirtualFile, Resource}
import org.bson.types.ObjectId
import controllers.{S3Service, Log}
import scala.xml._
import scala.Some
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import api.ApiError
import com.mongodb.casbah.Imports._
import play.api.test.FakeHeaders
import scala.Some
import play.api.mvc.AnyContentAsJson
import play.api.libs.json.JsObject
import controllers.auth.Permission
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import api.v1.ItemApi

class ItemApiTest extends BaseTest with Mockito {

  val mockS3service = mock[S3Service]

  val TEST_COLLECTION_ID: String = "51114b127fc1eaa866444647"
  //val OTHER_TEST_COLLECTION_ID: String = "5001a66ce4b0d7c9ec320f2e"

  val ItemRoutes = api.v1.routes.ItemApi

//  val accessToken = new AccessToken(new ObjectId("502404dd0364dc35bb39339c"),Some("homer"),"itemapi_test_token",DateTime.now(),DateTime.now().plusMinutes(5));
//  AccessToken.insert(accessToken)
//  override val token = "itemapi_test_token"

  val allItemsCount = 25

  "list all items" in {
    val call = ItemRoutes.list()
    val fakeRequest = FakeRequest(call.method, tokenize(call.url) )
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo(allItemsCount)
  }

"list items in a collection" in {
  val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
  val Some(result) = routeAndCall(fakeRequest)
  status(result) must equalTo(OK)
  charset(result) must beSome("utf-8")
  contentType(result) must beSome("application/json")
  val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
  items.size must beEqualTo(allItemsCount)
}

  "list all items skipping 3" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=3".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo(allItemsCount - 3)
  }

  "list items limiting result to 2" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=2".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo(2)
  }

  "find items in the grade level 7" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&q={\"gradeLevel\":\"07\"}".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size must beEqualTo(5)
  }

  "find items in returning only their title and up to 3" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=3".format(token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.foreach(i => {
      (i \ "title").as[Option[String]] must beSome
      (i \ "author").as[Option[String]] must beNone
    })
    items.size must beEqualTo(3)
  }

  "get an item by id" in {
    val id = "51116a8ba14f7b657a083c1c"
    val fakeRequest = FakeRequest(GET, "/api/v1/items/%s?access_token=%s".format(id, token))
    val Some(result) = routeAndCall(fakeRequest)
    status(result) must equalTo(OK)
    charset(result) must beSome("utf-8")
    contentType(result) must beSome("application/json")
    val item = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (item \ "id").as[String] must beEqualTo(id)
  }

  "create does not require a collection id" in {
    val toCreate = xmlBody("<html></html>")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val collectionId = (Json.fromJson[JsValue](Json.parse(contentAsString(result))) \ "collectionId").as[String]
    ContentCollection.findOneById(new ObjectId(collectionId)).get.name must beEqualTo(ContentCollection.DEFAULT)
  }

  "create requires an authorized collection id" in {
    val toCreate = xmlBody("<html></html>", Map("collectionId" -> "something"))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = routeAndCall(fakeRequest).get
    status(result) must equalTo(UNAUTHORIZED)
    val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
    (collection \ "code").as[Int] must equalTo(ApiError.CollectionUnauthorized.code)
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
    (collection \ "code").as[Int] must equalTo(ApiError.IdNotNeeded.code)
  }

  "update with a new collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = api.v1.routes.ItemApi.create()
    val createResult = routeAndCall(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)

    val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.collectionId -> TEST_COLLECTION_ID))
    val updateCall = api.v1.routes.ItemApi.update(new ObjectId(id))

    val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    val item: Item = Json.parse(contentAsString(updateResult)).as[Item]
    item.collectionId must equalTo(TEST_COLLECTION_ID)
  }


  "update with no collectionId returns the item's stored collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = api.v1.routes.ItemApi.create()
    val createResult = routeAndCall(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)
    val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.author -> "Ed"))

    val updateCall = api.v1.routes.ItemApi.update(new ObjectId(id))

    val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    Log.i(contentAsString(updateResult))
    val item: Item = Json.parse(contentAsString(updateResult)).as[Item]

    item.collectionId must equalTo(TEST_COLLECTION_ID)
  }

  val STATE_DEPT: String = "State Department of Education"

  "get and update return the same json" in {

    val toCreate = xmlBody("<root/>", Map(Item.collectionId -> TEST_COLLECTION_ID, Item.credentials -> STATE_DEPT))
    val call = api.v1.routes.ItemApi.create()
    val createResult = routeAndCall(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]

    val getItemCall = api.v1.routes.ItemApi.get(new ObjectId(id))
    val getResult = routeAndCall(FakeRequest(getItemCall.method, tokenize(getItemCall.url), FakeHeaders(), AnyContentAsEmpty)).get

    val getJsonString = contentAsString(getResult)

    val updateCall = api.v1.routes.ItemApi.update(new ObjectId(id))

    val toUpdate = xmlBody("<root/>", Map(Item.credentials -> STATE_DEPT))
    val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    val updateJsonString = contentAsString(updateResult)
    updateJsonString must equalTo(getJsonString)
  }


  "delete moves item to the archived collection" in {

    val item = Item(collectionId = TEST_COLLECTION_ID)
    Item.save(item)
    val deleteItemCall = api.v1.routes.ItemApi.delete(item.id)

    routeAndCall(tokenFakeRequest(deleteItemCall.method, deleteItemCall.url, FakeHeaders())) match {
      case Some(deleteResult) => {
        Item.findOneById(item.id) match {
          case Some(deletedItem) => deletedItem.collectionId must equalTo(models.ContentCollection.archiveCollId.toString)
          case _ => failure("couldn't find deleted item")
        }
      }
      case _ => failure("delete failed")
    }
  }



  "when saving an item with QTI xml, add csFeedbackId attrs if they are not present" in {
    /**
     * all feedback elements, feedbackInline and modalFeedback should be decorated with the attribute csFeedbackId
     * on persist/update of an item if the attribute is not already there. These need to be unique within the item, so itemId-n would work as id
     *
     * NOTE: test data loaded to db may be missing this if it is loaded statically. Could load a the test composite item
     * (50083ba9e4b071cb5ef79101) and save it. Other tests might fail if these csFeedbackId attrs are not present
     */
    var jsitem = Json.toJson(Item.findOneById(new ObjectId("511156d38604c9f77da9739d"))).asInstanceOf[JsObject]
    jsitem = JsObject(jsitem.fields.filter(field => field._1 != "id" && field._1 != "collectionId"))
    var fakeRequest = FakeRequest(PUT, "/api/v1/items/511156d38604c9f77da9739d?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(jsitem))
    var result = routeAndCall(fakeRequest).get
    status(result) must equalTo(OK)
    val resource: Resource = Item.findOneById(new ObjectId("511156d38604c9f77da9739d")).get.data.get
    val qtiXml: Option[String] = resource.files.find(file => file.isMain).map(file => file.asInstanceOf[VirtualFile].content)
    qtiXml must beSome[String]
    val hasCsFeedbackIds: Boolean = qtiXml.map(qti =>
      findFeedbackIds(XML.loadString(qti), Seq(), 0).find(result => {
        if (result._2) false else true
      }).isEmpty
    ).getOrElse(false)
    hasCsFeedbackIds must beTrue
  }

  private def findFeedbackIds(xml: Elem, acc: Seq[(NodeSeq, Boolean)], levels: Int): Seq[(NodeSeq, Boolean)] = {
    var feedback: Seq[(NodeSeq, Boolean)] = acc
    val children = xml.child
    for (child <- children) {
      val feedbackInline = child \ "feedbackInline"
      if (feedbackInline.isEmpty) {
        child match {
          case innerXml: Elem => feedback = findFeedbackIds(innerXml, feedback, levels + 1)
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

  "clone item" in {
    val itemApi = new ItemApi(mockS3service)
    val id = "511154e48604c9f77da9739b"
    val fakeRequest = FakeRequest(POST, "/api/v1/items/%s?access_token=%s".format(id, token))
    val result = itemApi.cloneItem(new ObjectId(id))(fakeRequest)
    there was atLeastTwo(mockS3service).cloneFile(anyString, anyString, anyString)
  }

}
