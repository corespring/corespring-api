package tests

import _root_.web.controllers.utils.ConfigLoader
import _root_.models.item.Item
import org.specs2.mutable.Specification
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import scala.Some
import scala.Some
import play.api.test.FakeHeaders
import scala.Some
import org.bson.types.ObjectId
import common.seed.SeedDb
import org.specs2.specification.{Step, Fragments}


/**
 * Base class for tests
 *
 */
trait BaseTest extends Specification {

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
  PlaySingleton.start()
  override def map(fs: => Fragments) = Step(initDB) ^ fs

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

    Item.findOneById(new ObjectId(id)) match {
      case Some(item) => {
        item
      }
      case _ => throw new RuntimeException("test item")
    }
  }


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
  def xmlBody(xml: String, attributes: Map[String, String] = Map()): JsValue = {
    Json.toJson(attributes)
    //removed because new item retrieval does not return data
//    Json.toJson(
//      attributes.iterator.foldLeft(
//        Map(
//          Item.data -> Json.toJson(
//            Map(
//              "name" -> JsString("qtiItem"),
//              "files" -> Json.toJson(
//                Seq(
//                  Json.toJson(
//                    Map(
//                      "name" -> Json.toJson("qti.xml"),
//                      "default" -> Json.toJson(false),
//                      "contentType" -> Json.toJson("text/xml"),
//                      "content" -> Json.toJson(xml)
//                    )
//                  )
//                )
//              )
//            )
//          )
//        ))((map, entry) => map + ((entry._1, Json.toJson(entry._2))))
//    )
  }

  def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => { (file \ "content").toString })
  }

}
