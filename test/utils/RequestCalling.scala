package utils

import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._
import common.log.PackageLogging

trait RequestCalling extends PackageLogging {

  val accessToken = "test_token"

  lazy val FakeAuthHeader = FakeHeaders(Seq("Authorization" -> Seq("Bearer " + accessToken)))

  /** Invoke a fake request - parse the response from json to type T
    * @return the typed instance that was returned
    */
  def invokeCall[T](action: Action[AnyContent], content: AnyContent)(implicit reads: Reads[T], writes: Writes[T]): T = {

    val request : Request[AnyContent] = FakeRequest(
      "ignore",
      "ignore",
      FakeAuthHeader,
      content)

    val result: Result = action(request)

    if (status(result) == OK) {
      val json: JsValue = Json.parse(contentAsString(result))
      getData(reads.reads(json))
    } else {
      Logger.warn(s"Error invoking call: ${contentAsString(result)}")
      getData(reads.reads(JsObject(Seq())))
    }
  }

  def getData[A](maybeData:JsResult[A]) : A = maybeData match {
    case JsSuccess(d,_) => d
    case _ => throw new RuntimeException("couldn't read json")
  }
}
