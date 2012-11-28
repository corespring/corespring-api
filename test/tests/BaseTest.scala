package tests

import _root_.models.Item
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


/**
 * Base class for tests
 *
 */
abstract class BaseTest extends Specification {

  // From standard fixture data
  val token = "34dj45a769j4e1c0h4wb"

  /**
   * Decorate play.api.mvc.Result with some helper methods
   */
  class ResultHelper(result: Result) {
    def \(key: String): String = (Json.parse(contentAsString(result)) \ key).as[String]

    def body: String = contentAsString(result).toString
  }

  implicit def resultToResultHelper(result: Result) = new ResultHelper(result)


  PlaySingleton.start()

  def tokenize(url: String): String = url + "?access_token=" + token

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
                      "name" -> Json.toJson("qti.xml"),
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

  def getXMLContentFromResponse(jsonResponse: String): Seq[String] = {
    (Json.parse(jsonResponse) \ Item.data \ "files").asOpt[Seq[JsObject]].getOrElse(Seq()).map(file => { (file \ "content").toString })
  }

}
