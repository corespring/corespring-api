package utils

import play.api.mvc.{Results, Result, AnyContent}
import play.mvc.Call
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.FakeHeaders

trait RequestCalling {

  val token = "test_token"

  lazy val FakeAuthHeader = FakeHeaders(Map("Authorization" -> Seq("Bearer " + token)))

  /** Invoke a fake request - parse the response from json to type T
    * @param call
    * @param content
    * @param args
    * @param reads
    * @param writes
    * @tparam T
    * @return the typed instance that was returned
    */
  def invokeCall[T](call: Call, content: AnyContent, args: (String, String)*)(implicit reads: Reads[T], writes: Writes[T]): T = {

    val url = call.url + "?" + args.toList.map((a: (String, String)) => a._1 + "=" + a._2).mkString("&")
    val request = FakeRequest(
      call.method,
      url,
      FakeAuthHeader,
      content)

    val result: Result = routeAndCall(request).get

    if (status(result) == OK) {
      val json: JsValue = Json.parse(contentAsString(result))
      reads.reads(json)
    } else {
      reads.reads(JsObject(Seq()))
    }
  }
}
