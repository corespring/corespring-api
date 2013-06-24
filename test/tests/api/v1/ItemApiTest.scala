package tests.api.v1

import api.ApiError
import api.v1.NewItemApi
import com.mongodb.casbah.Imports._
import common.log.PackageLogging
import controllers.S3Service
import models._
import models.item.Item
import models.item.Item.Keys
import models.item.resource.{Resource, VirtualFile}
import models.item.service.{ItemService, ItemServiceClient}
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import scala.xml._
import tests.BaseTest

class ItemApiTest extends BaseTest with Mockito with PackageLogging with ItemServiceClient {

  val mockS3service = mock[S3Service]

  val TEST_COLLECTION_ID: String = "51114b127fc1eaa866444647"

  val ItemRoutes = api.v1.routes.ItemApi
  val NewItemRoutes = api.v1.routes.NewItemApi

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
    val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))
    items.size === count
  }

  def assertSingleResult(result: Result, block: JsValue => org.specs2.execute.Result) = {
    forall(assertBasics(result))(r =>
      r.isSuccess === true
    )
    block(Json.fromJson[JsValue](Json.parse(contentAsString(result))))
  }

  "list" should {

    "return all items" in {
      val call = ItemRoutes.list()
      val fakeRequest = FakeRequest(call.method, tokenize(call.url))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, allItemsCount)
    }

    "by collection" in {
      val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s".format(token))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, allItemsCount)
    }

    "return all - skipping 3" in {
      val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&sk=3".format(token))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, allItemsCount - 3)
    }

    "return all limiting to 2" in {
      val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&l=2".format(token))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, 2)
    }
  }


  "find" should {

    "return items with grade level 7" in {
      val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&q={\"gradeLevel\":\"07\"}".format(token))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, 6)
    }

    "return only titles and limit set to 3" in {
      val fakeRequest = FakeRequest(GET, "/api/v1/items?access_token=%s&f={\"title\":1}&l=3".format(token))
      val Some(result) = routeAndCall(fakeRequest)
      assertResult(result, 3)
      val items = Json.fromJson[List[JsValue]](Json.parse(contentAsString(result)))

      forall(items)(i => {
        (i \ "title").as[Option[String]] must beSome
        (i \ "author").as[Option[String]] must beNone
      })
    }

  }

  "get" should {

    "return an item by id" in {
      val id = "51116a8ba14f7b657a083c1c"
      val fakeRequest = FakeRequest(GET, "/api/v1/items/%s?access_token=%s".format(id, token))
      val Some(result) = routeAndCall(fakeRequest)
      assertSingleResult(result, (json: JsValue) => (json \ "id").as[String] === id)
    }

  }

  "create" should {

    "not require a collection id" in {
      val toCreate = xmlBody("<html></html>")
      val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
      val result = routeAndCall(fakeRequest).get

      assertSingleResult(result, (json) => {
        val collectionId = (json \ "collectionId").as[String]
        ContentCollection.findOneById(new ObjectId(collectionId)).get.name === ContentCollection.DEFAULT
      })
    }

    "requires an authorized collection id" in {
      val toCreate = xmlBody("<html></html>", Map("collectionId" -> "something"))
      val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
      val result = routeAndCall(fakeRequest).get
      status(result) must equalTo(UNAUTHORIZED)
      val collection = Json.fromJson[JsValue](Json.parse(contentAsString(result)))
      (collection \ "code").as[Int] must equalTo(ApiError.CollectionUnauthorized.code)
    }

    "not include csFeedbackIds" in {
      val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
      val fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
      val result = routeAndCall(fakeRequest).get
      status(result) === OK
      val xmlData = (Json.parse(contentAsString(result)) \ Keys.data).toString
      xmlData must not(beMatching(".*csFeedbackId.*"))
    }

    "not accept id" in {
      val toCreate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("collectionId" -> TEST_COLLECTION_ID))
      var fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toCreate))
      var result = routeAndCall(fakeRequest).get
      status(result) === OK
      val itemId = (Json.parse(contentAsString(result)) \ "id").toString

      val toUpdate = xmlBody("<html><feedbackInline></feedbackInline></html>", Map("id" -> itemId))
      fakeRequest = FakeRequest(POST, "/api/v1/items?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(toUpdate))
      result = routeAndCall(fakeRequest).get
      var collection = Json.parse(contentAsString(result))
      (collection \ "code").as[Int] must equalTo(ApiError.IdNotNeeded.code)
    }

  }



  "update" should {

    def request(content: AnyContent) = FakeRequest("", tokenize(""), FakeHeaders(), content)

    val toCreate = xmlBody("<root/>", Map("collectionId" -> TEST_COLLECTION_ID))

    val toUpdate = xmlBody("<root2/>", Map(Keys.collectionId -> TEST_COLLECTION_ID))

    "work with a new collection id" in {
      val createResult = api.v1.ItemApi.create()(request(AnyContentAsJson(toCreate)))
      val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]
      val updateResult = api.v1.NewItemApi.update(versionedId(id))(request(AnyContentAsJson(toUpdate)))
      val item: Item = Json.parse(contentAsString(updateResult)).as[Item]
      item.collectionId === TEST_COLLECTION_ID
    }

    "return the item's stored collection id" in {
      val createResult = api.v1.ItemApi.create()(request(AnyContentAsJson(toCreate)))
      val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]
      val toUpdate = xmlBody("<root2/>", Map(Keys.author -> "Ed"))
      val updateResult = api.v1.NewItemApi.update(versionedId(id))(request(AnyContentAsJson(toUpdate)))
      val item: Item = Json.parse(contentAsString(updateResult)).as[Item]
      item.collectionId must equalTo(TEST_COLLECTION_ID)
    }

    val STATE_DEPT: String = "State Department of Education"

    "get and update return the same json" in {

      val toCreate = xmlBody("<root/>", Map(Keys.collectionId -> TEST_COLLECTION_ID, Keys.credentials -> STATE_DEPT))
      val call = api.v1.routes.ItemApi.create()
      val createResult = routeAndCall(FakeRequest(call.method, tokenize(call.url), FakeHeaders(), AnyContentAsJson(toCreate))).get
      val id = (Json.parse(contentAsString(createResult)) \ "id").as[String]

      val getItemCall = NewItemRoutes.get(versionedId(id))
      val getResult = routeAndCall(FakeRequest(getItemCall.method, tokenize(getItemCall.url), FakeHeaders(), AnyContentAsEmpty)).get

      val getJsonString = contentAsString(getResult)

      val updateCall = NewItemRoutes.update(versionedId(id))

      val toUpdate = xmlBody("<root/>", Map(Keys.credentials -> STATE_DEPT))
      val updateResult = routeAndCall(FakeRequest(updateCall.method, tokenize(updateCall.url), FakeHeaders(), AnyContentAsJson(toUpdate))).get
      val updateJsonString = contentAsString(updateResult)
      updateJsonString must equalTo(getJsonString)
    }

  }


  "delete" should {
    "move item to the archived collection" in {

      val item = Item(collectionId = TEST_COLLECTION_ID)
      itemService.save(item)
      val deleteItemCall = api.v1.routes.ItemApi.delete(item.id)

      routeAndCall(tokenFakeRequest(deleteItemCall.method, deleteItemCall.url, FakeHeaders())) match {
        case Some(deleteResult) => {
          itemService.findOneById(item.id) match {
            case Some(deletedItem) => deletedItem.collectionId must equalTo(models.ContentCollection.archiveCollId.toString)
            case _ => failure("couldn't find deleted item")
          }
        }
        case _ => failure("delete failed")
      }
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
    var jsitem = Json.toJson(itemService.findOneById(versionedId("511156d38604c9f77da9739d"))).asInstanceOf[JsObject]
    jsitem = JsObject(jsitem.fields.filter(field => field._1 != "id" && field._1 != "collectionId"))
    var fakeRequest = FakeRequest(PUT, "/api/v1/items/511156d38604c9f77da9739d?access_token=%s".format(token), FakeHeaders(), AnyContentAsJson(jsitem))
    var result = routeAndCall(fakeRequest).get
    status(result) === OK
    val resource: Resource = itemService.findOneById(versionedId("511156d38604c9f77da9739d")).get.data.get
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

  "clone" should {
    "clone item" in {
      val itemApi = new NewItemApi {
        def itemService: ItemService = itemService

        def s3service: S3Service = mockS3service

        def bucket: String = "blah"
      }

      val id = "511154e48604c9f77da9739b"
      val fakeRequest = FakeRequest(POST, "/api/v1/items/%s?access_token=%s".format(id, token))
      itemApi.cloneItem(versionedId(id))(fakeRequest)
      there was atLeastTwo(mockS3service).cloneFile(anyString, anyString, anyString)
    }
  }

}
