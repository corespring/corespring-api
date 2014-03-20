package org.corespring.api.v1

import com.mongodb.casbah.Imports._
import org.corespring.api.v1.errors.ApiError
import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.BaseTest
import org.corespring.test.helpers.FixtureData
import org.corespring.test.helpers.models.ItemHelper
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.concurrent.Future
import scala.xml._

/**
 * Putting the fixture generating test into its own test class
 * Going forward we should be using generated data for individual tests
 */
class NewItemApiTest extends BaseTest {
  "list items in a collection" in new FixtureData {
    println(s"[Test] collection: $collectionId, token: $accessToken")
    //TODO: Don't use magic strings for the routes - call the controller directly
    val fakeRequest = FakeRequest("", s"?access_token=$accessToken")
    val result = ItemApi.listWithColl(collectionId, None, None, "false", 0, 50, None)(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(itemIds.length)
  }
}

class ItemApiTest extends BaseTest with Mockito {

  sequential

  val mockS3service = mock[CorespringS3Service]

  val ItemRoutes = org.corespring.api.v1.routes.ItemApi

  val DefaultPageSize = 50

  def assertBasics(result: Future[SimpleResult]): List[org.specs2.execute.Result] = {
    List(
      status(result) === OK,
      charset(result) === Some("utf-8"),
      contentType(result) === Some("application/json"))
  }

  def assertResult(result: Future[SimpleResult], count: Int): org.specs2.execute.Result = {
    forall(assertBasics(result))(r =>
      r.isSuccess === true)
    val json = Json.parse(contentAsString(result))

    val items = jsonToObj[List[JsValue]](contentAsString(result), List())

    items.size === count
  }

  def assertSingleResult(result: Future[SimpleResult], block: JsValue => org.specs2.execute.Result) = {
    forall(assertBasics(result))(r =>
      r.isSuccess === true)
    val thing = jsonToObj[JsValue](contentAsString(result), JsObject(Seq()))
    block(thing)
  }

  def jsonToObj[A](s: String, default: A)(implicit reads: Reads[A]): A = {
    Json.parse(s).as[A] match {
      case JsSuccess(v, _) => v.asInstanceOf[A]
      case JsError(e) => default
    }
  }

  "list all items" in {
    val call = ItemRoutes.list()
    val fakeRequest = FakeRequest(call.method, tokenize(call.url))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(List(ItemHelper.publicCount, DefaultPageSize).min)
  }

  "list all items skipping 3" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=3".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(List(ItemHelper.publicCount - 3, DefaultPageSize).min)
  }

  "list items limiting result to 2" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=2".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(2)
  }

  "find items in the grade level 7" in {
    pending("2.1.1 upgrade to be added")
    true === true
  }

  "find items in returning only their title and up to 3" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=3".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.foreach(i => {
      (i \ "title").as[Option[String]] must beSome
      (i \ "author").as[Option[String]] must beNone
    })
    items.size must beEqualTo(3)
  }

  "get an item by id" in {
    val id = "51116a8ba14f7b657a083c1c:0"
    val fakeRequest = FakeRequest(GET, "/api/v1/items/%s?access_token=%s".format(id, token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val item = parsed[JsValue](result)
    (item \ "id").as[String] must beEqualTo(id)
  }

  "create does not require a collection id" in {
    val toCreate = xmlBody("<html></html>")
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = route(fakeRequest).get
    assertResult(result)
    val item = parsed[JsValue](result)
    val collectionId = (item \ "collectionId").as[String]
    ContentCollection.findOneById(new ObjectId(collectionId)).get.name must beEqualTo(ContentCollection.DEFAULT)
  }

  "create requires an authorized collection id" in {
    val toCreate = xmlBody("<html></html>", Map("collectionId" -> "something"))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = route(fakeRequest).get
    status(result) must equalTo(UNAUTHORIZED)
    val collection = parsed[JsValue](result)
    (collection \ "code").as[Int] must equalTo(ApiError.CollectionUnauthorized.code)
  }

  "create response does not include csFeedbackIds" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    val result = route(fakeRequest).get
    status(result) must equalTo(OK)
    val xmlData = (parsed[JsValue](result) \ Item.Keys.data).toString()
    xmlData must not(beMatching(".*csFeedbackId.*"))
  }

  "create does not accept id" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = route(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (parsed[JsValue](result) \ "id").toString()

    val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("id" -> itemId))
    fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toUpdate))
    result = route(fakeRequest).get
    val collection = parsed[JsValue](result)
    (collection \ "code").as[Int] must equalTo(ApiError.IdNotNeeded.code)
  }

  "update with a new collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = ItemRoutes.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)

    val id = (parsed[JsValue](createResult) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.Keys.collectionId -> TEST_COLLECTION_ID))
    val updateCall = ItemRoutes.update(VersionedId(new ObjectId(id)))

    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    val item: Item = parsed[Item](updateResult)
    item.collectionId must equalTo(Some(TEST_COLLECTION_ID))
  }

  "update with no collectionId returns the item's stored collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = ItemRoutes.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)
    val id = (parsed[JsValue](createResult) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.Keys.author -> "Ed"))

    val updateCall = ItemRoutes.update(VersionedId(new ObjectId(id)))

    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    val item: Item = parsed[Item](updateResult)

    item.collectionId must equalTo(Some(TEST_COLLECTION_ID))
  }

  val STATE_DEPT: String = "State Department of Education"

  "get and update return the same json" in {

    val m: Map[String, String] = Map(Item.Keys.collectionId -> TEST_COLLECTION_ID, "credentials" -> STATE_DEPT)
    val toCreate = xmlBody("<root/>", m)
    val call = ItemRoutes.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    val id = (parsed[JsValue](createResult) \ "id").as[String]

    val getItemCall = ItemRoutes.get(VersionedId(new ObjectId(id)))
    val getResult = route(FakeRequest(getItemCall.method, tokenize(getItemCall.url), FakeHeaders(), AnyContentAsEmpty)).get

    val getJsonString = contentAsString(getResult)

    val updateCall = ItemRoutes.update(VersionedId(new ObjectId(id)))

    val toUpdate = xmlBody("<root/>", Map(Item.Keys.credentials -> STATE_DEPT))
    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    val updateJsonString = contentAsString(updateResult)
    updateJsonString must equalTo(getJsonString)
  }

  "delete moves item to the archived collection" in {

    val item = Item(collectionId = Some(TEST_COLLECTION_ID))
    ItemServiceWired.save(item)
    val deleteItemCall = ItemRoutes.delete(item.id)

    route(tokenFakeRequest(deleteItemCall.method, deleteItemCall.url, FakeHeaders())) match {
      case Some(deleteResult) => {
        status(deleteResult) === OK
        ItemServiceWired.findOneById(item.id) match {
          case Some(deletedItem) => deletedItem.collectionId === Some(ContentCollection.archiveCollId.toString)
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
    var jsitem = Json.toJson(ItemServiceWired.findOneById(VersionedId(new ObjectId("511156d38604c9f77da9739d")))).asInstanceOf[JsObject]
    jsitem = JsObject(jsitem.fields.filter(field => field._1 != "id" && field._1 != "collectionId"))
    val fakeRequest = FakeRequest(PUT, "/api/v1/items/511156d38604c9f77da9739d?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(jsitem))
    val result = route(fakeRequest).get
    status(result) must equalTo(OK)
    val resource: Resource = ItemServiceWired.findOneById(VersionedId(new ObjectId("511156d38604c9f77da9739d"))).get.data.get
    val qtiXml: Option[String] = resource.files.find(file => file.isMain).map(file => file.asInstanceOf[VirtualFile].content)
    qtiXml must beSome[String]
    val hasCsFeedbackIds: Boolean = qtiXml.map(qti =>
      findFeedbackIds(XML.loadString(qti), Seq(), 0).find(result => {
        if (result._2) false else true
      }).isEmpty).getOrElse(false)
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
      } else {
        val feedbackInlines = feedbackInline.theSeq
        for (feedbackNode <- feedbackInlines) {
          if ((feedbackNode \ "@csFeedbackId").nonEmpty) {
            feedback = feedback :+ (feedbackInline -> true)
          } else {
            feedback = feedback :+ (feedbackInline -> false)
          }
        }
      }
    }
    feedback
  }

  "clone item" in {
    val itemApi = new ItemApi(mockS3service, ItemServiceWired, dependencies.metadataSetService)
    val id = "511154e48604c9f77da9739b"
    val fakeRequest = FakeRequest(POST, "/api/v1/items/%s?access_token=%s".format(id, token))
    val result = itemApi.cloneItem(VersionedId(new ObjectId(id)))(fakeRequest)
    there was atLeastTwo(mockS3service).copyFile(anyString, anyString, anyString)
  }.pendingUntilFixed("Play 2.1.3 upgrade - fix this")

  "updating item metadata without having a corresponding set results in error" in {
    val itemId = "511154e48604c9f77da9739b"
    val property = "flergl.mergl"
    val fakeRequest = FakeRequest(PUT, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token)).withTextBody("the answer to all things")
    val result = route(fakeRequest).get
    status(result) must beEqualTo(BAD_REQUEST)
    result.body must contain(ApiError.MetadataNotFound.message)
  }
  "updating item metadata with incorrect schema results in error" in {
    val itemId = "511154e48604c9f77da9739b"
    val property = "blergl.flergl"
    val fakeRequest = FakeRequest(PUT, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token)).withTextBody("the answer to all things")
    val result = route(fakeRequest).get
    status(result) must beEqualTo(BAD_REQUEST)
    result.body must contain(ApiError.MetadataNotFound.message)
  }
  "update/retrieve single property metadata with corresponding set" in {
    val itemId = "511154e48604c9f77da9739b"
    val property = "blergl.mergl"
    val value = "the answer to all things"
    val updateRequest = FakeRequest(PUT, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token)).withTextBody(value)
    val updateResult = route(updateRequest).get
    status(updateResult) must beEqualTo(OK)
    val updateJson = Json.parse(updateResult.body)
    (updateJson \ "blergl" \ "mergl").as[String] must beEqualTo(value)
    val request = FakeRequest(GET, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token))
    val requestResult = route(request).get
    status(requestResult) must beEqualTo(OK)
    val requestJson = Json.parse(requestResult.body)
    (requestJson \ "mergl").as[String] must beEqualTo(value)
  }
  "update/retrieve properties of item metadata set" in {
    val itemId = "511154e48604c9f77da9739b"
    val property = "blergl"
    val body = Json.obj(
      "mergl" -> "the answer to all things",
      "platypus" -> "greatest animal ever",
      "baaaa" -> "sound a goat makes")
    val updateRequest = FakeRequest(PUT, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token)).withJsonBody(body)
    val updateResult = route(updateRequest).get
    status(updateResult) must beEqualTo(OK)
    val json = Json.parse(updateResult.body)
    json match {
      case JsObject(fields) => fields.find(_._1 == property) match {
        case Some((_, jsprops)) => jsprops match {
          case JsObject(props) => {
            props.forall(prop => body.fields.find(field => field._1 == prop._1).isDefined) must beTrue
          }
          case _ => failure("metadata did not have a properties object")
        }
        case None => failure("could not find metadata properties with key " + property)
      }
      case _ => failure("returned json not an object")
    }
    val request = FakeRequest(GET, "/api/v1/items/%s/extended/%s?access_token=%s".format(itemId, property, token))
    val requestResult = route(request).get
    status(requestResult) must beEqualTo(OK)
    val requestJson = Json.parse(requestResult.body)
    requestJson match {
      case JsObject(fields) => body.fields.forall(prop => {
        fields.find(_._1 == prop._1).isDefined
      }) must beTrue
      case _ => failure("returned json not an object")
    }
  }
}