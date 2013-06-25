package tests

import _root_.common.seed.SeedDb
import _root_.models.item.Item
import _root_.models.item.resource.StoredFile
import _root_.models.item.service.{ItemService, ItemServiceClient, ItemServiceImpl}
import _root_.web.controllers.utils.ConfigLoader
import helpers.TestS3Service
import org.bson.types.ObjectId
import org.specs2.mutable.Specification
import org.specs2.specification.{Step, Fragments}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.Some
import org.corespring.platform.data.mongo.models.VersionedId
import com.mongodb.casbah.Imports._
import play.api.test.FakeHeaders
import scala.Some
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.mvc.AnyContentAsJson
import play.api.mvc.AnyContentAsText
import play.api.libs.json.JsObject
import controllers.{S3Service, S3ServiceClient}


/**
 * Base class for tests
 *
 */
trait BaseTest extends Specification with ItemServiceClient with S3ServiceClient{

  def itemService : ItemService = ItemServiceImpl
  def s3Service: S3Service = TestS3Service

  // From standard fixture data
  val token = "test_token"

  val isLocalDb: Boolean = {
    ConfigLoader.get("mongodb.default.uri") match {
      case Some(url) => (url.contains("localhost") || url.contains("127.0.0.1") || url == "mongodb://bleezmo:Basic333@ds035907.mongolab.com:35907/sib")
      case None => false
    }
  }
  def initDB = if (isLocalDb){
    SeedDb.emptyData()
    SeedDb.seedData("conf/seed-data/test")
  }else{
    throw new RuntimeException("You're trying to seed against a remote db - bad idea")
  }
  def initS3 = {
    TestS3Service.init
    val s3files = TestS3Service.files(s3Service.bucket)
    itemService.find(MongoDBObject()).foreach(item => {
      val storedFiles:Seq[StoredFile] =
        item.data.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).getOrElse(Seq()) ++
          item.supportingMaterials.map(r => r.files.filter(_.isInstanceOf[StoredFile]).map(_.asInstanceOf[StoredFile])).flatten
      storedFiles.foreach(sf => {
        if(!s3files.contains(sf.storageKey)){

        }
      })
    })
  }
  PlaySingleton.start()
  override def map(fs: => Fragments) = Step(initDB) ^ Step(initS3) ^ fs

  /**
   * Decorate play.api.mvc.Result with some helper methods
   */
  class ResultHelper(result: Result) {
    def \(key: String): String = (Json.parse(contentAsString(result)) \ key).as[String]

    def body: String = contentAsString(result).toString
  }

  implicit def resultToResultHelper(result: Result) = new ResultHelper(result)

  def tokenize(url: String, tkn : String = token): String = url + "?access_token=" + tkn

  def tokenFakeRequest[A](method: String, uri: String, headers: FakeHeaders = FakeHeaders(), body: A = AnyContentAsText("")): FakeRequest[A] = {
    FakeRequest(method, tokenize(uri), headers, body)
  }

  /**
   * @param id item id
   * @return
   */
  def item(id:String): Item = {

    itemService.findOneById(VersionedId(new ObjectId(id))) match {
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
    routeAndCall(request)
  }

  /**
   * Generates JSON request body for the API, with provided XML data in the appropriate field. Also adds in a set of
   * top-level attributes that get added to the request.
   */
  def xmlBody(xml: String, attributes: Map[String, String] = Map()): JsValue = Json.toJson(attributes)

  def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.Keys.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => { (file \ "content").toString })
  }

}
