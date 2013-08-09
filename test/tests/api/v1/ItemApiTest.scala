package tests.api.v1

import api.ApiError
import api.v1.ItemApi
import com.mongodb.casbah.Imports._
import org.corespring.assets.CorespringS3Service
import org.corespring.platform.core.models.ContentCollection
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.{VirtualFile, Resource}
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.BaseTest
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.xml._

class ItemApiTest extends BaseTest with Mockito {

  val mockS3service = mock[CorespringS3Service]


  val ItemRoutes = api.v1.routes.ItemApi

  //TODO: We shouldn't be depending on a magic number here
  //We should seed our own collection instead so we have complete control over the data.
  val allItemsCount = 26

  def assertBasics(result: Result): List[org.specs2.execute.Result] = {
    List(
      status(result) === OK,
      charset(result) === Some("utf-8"),
      contentType(result) === Some("application/json"))
  }

  def assertResult(result: Result, count: Int): org.specs2.execute.Result = {
    forall(assertBasics(result))(r =>
      r.isSuccess === true
    )
    val json = Json.parse(contentAsString(result))

    val items = jsonToObj[List[JsValue]](contentAsString(result), List())

    items.size === count
  }

  def assertSingleResult(result: Result, block: JsValue => org.specs2.execute.Result) = {
    forall(assertBasics(result))(r =>
      r.isSuccess === true
    )
    val thing = jsonToObj[JsValue](contentAsString(result), JsObject(Seq()))
    block(thing)
  }

  def jsonToObj[A](s:String, default: A)(implicit reads : Reads[A]) : A = {
    Json.parse(s).as[A] match {
      case JsSuccess(v , _) => v.asInstanceOf[A]
      case JsError(e) => default
    }
  }

  "list all items" in {
    val call = ItemRoutes.list()
    val fakeRequest = FakeRequest(call.method, tokenize(call.url) )
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(allItemsCount)
  }

"list items in a collection" in {
  val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
  val Some(result) = route(fakeRequest)
  assertResult(result)
  val items = parsed[List[JsValue]](result)
  items.size must beEqualTo(allItemsCount)
}

  "list all items skipping 3" in {
    val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=3".format(token))
    val Some(result) = route(fakeRequest)
    assertResult(result)
    val items = parsed[List[JsValue]](result)
    items.size must beEqualTo(allItemsCount - 3)
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
    val xmlData = (parsed[JsValue](result)  \ Item.Keys.data).toString()
    xmlData must not(beMatching(".*csFeedbackId.*"))
  }

  "create does not accept id" in {
    val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
    var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
    var result = route(fakeRequest).get
    status(result) must equalTo(OK)
    val itemId = (parsed[JsValue](result)\ "id").toString()

    val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("id" -> itemId))
    fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toUpdate))
    result = route(fakeRequest).get
    val collection = parsed[JsValue](result)
    (collection \ "code").as[Int] must equalTo(ApiError.IdNotNeeded.code)
  }

  "update with a new collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = api.v1.routes.ItemApi.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)

    val id = (parsed[JsValue](createResult) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.Keys.collectionId -> TEST_COLLECTION_ID))
    val updateCall = api.v1.routes.ItemApi.update(VersionedId(new ObjectId(id)))

    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    val item: Item = parsed[Item](updateResult)
    item.collectionId must equalTo(TEST_COLLECTION_ID)
  }


  "update with no collectionId returns the item's stored collection id" in {

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))
    val call = api.v1.routes.ItemApi.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    status(createResult) must equalTo(OK)
    val id = (parsed[JsValue](createResult) \ "id").as[String]
    val toUpdate = xmlBody("<root2/>", Map(Item.Keys.author -> "Ed"))

    val updateCall = api.v1.routes.ItemApi.update(VersionedId(new ObjectId(id)))

    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    status(updateResult) must equalTo(OK)
    val item: Item = parsed[Item](updateResult)

    item.collectionId must equalTo(TEST_COLLECTION_ID)
  }

  val STATE_DEPT: String = "State Department of Education"

  "get and update return the same json" in {

    val m : Map[String,String] = Map(Item.Keys.collectionId -> TEST_COLLECTION_ID, "credentials" -> STATE_DEPT)
    val toCreate = xmlBody("<root/>",  m )
    val call = api.v1.routes.ItemApi.create()
    val createResult = route(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
    val id = (parsed[JsValue](createResult) \ "id").as[String]

    val getItemCall = api.v1.routes.ItemApi.get(VersionedId(new ObjectId(id)))
    val getResult = route(FakeRequest(getItemCall.method, tokenize(getItemCall.url), FakeHeaders(), AnyContentAsEmpty)).get

    val getJsonString = contentAsString(getResult)

    val updateCall = api.v1.routes.ItemApi.update(VersionedId(new ObjectId(id)))

    val toUpdate = xmlBody("<root/>", Map(Item.Keys.credentials -> STATE_DEPT))
    val updateResult = route(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
    val updateJsonString = contentAsString(updateResult)
    updateJsonString must equalTo(getJsonString)
  }


  "delete moves item to the archived collection" in {

    val item = Item(collectionId = TEST_COLLECTION_ID)
    ItemServiceImpl.save(item)
    val deleteItemCall = api.v1.routes.ItemApi.delete(item.id)

    route(tokenFakeRequest(deleteItemCall.method, deleteItemCall.url, FakeHeaders())) match {
      case Some(deleteResult) => {
        status(deleteResult) === OK
        ItemServiceImpl.findOneById(item.id) match {
          case Some(deletedItem) => deletedItem.collectionId === ContentCollection.archiveCollId.toString
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
    var jsitem = Json.toJson(ItemServiceImpl.findOneById(VersionedId(new ObjectId("511156d38604c9f77da9739d")))).asInstanceOf[JsObject]
    jsitem = JsObject(jsitem.fields.filter(field => field._1 != "id" && field._1 != "collectionId"))
    val fakeRequest = FakeRequest(PUT, "/api/v1/items/511156d38604c9f77da9739d?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(jsitem))
    val result = route(fakeRequest).get
    status(result) must equalTo(OK)
    val resource: Resource = ItemServiceImpl.findOneById(VersionedId(new ObjectId("511156d38604c9f77da9739d"))).get.data.get
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
    val itemApi = new ItemApi(mockS3service, ItemServiceImpl)
    val id = "511154e48604c9f77da9739b"
    val fakeRequest = FakeRequest(POST, "/api/v1/items/%s?access_token=%s".format(id, token))
    val result = itemApi.cloneItem(VersionedId(new ObjectId(id)))(fakeRequest)
    there was atLeastTwo(mockS3service).copyFile(anyString, anyString, anyString)
  }.pendingUntilFixed("Play 2.1.3 upgrade - fix this")

}
