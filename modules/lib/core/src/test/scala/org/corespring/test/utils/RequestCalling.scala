package org.corespring.test.utils

import org.corespring.common.log.PackageLogging
import play.api.libs.json._
import play.api.mvc._
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import play.api.test.Helpers._

trait RequestCalling extends PackageLogging with JsonToModel {

  val accessToken = "test_token"

  lazy val FakeAuthHeader = FakeHeaders(Seq("Authorization" -> Seq("Bearer " + accessToken)))

  /**
   * Invoke a fake request - parse the response from json to type T
   * @return the typed instance that was returned
   */
  def invokeCall[T](action: Action[AnyContent], content: AnyContent)(implicit reads: Reads[T], writes: Writes[T]): T = {

    val request: Request[AnyContent] = FakeRequest(
      "ignore",
      "ignore",
      FakeAuthHeader,
      content)

    val result: Result = action(request)

    if (status(result) == OK) {
      val json: JsValue = Json.parse(contentAsString(result))
      getData(reads.reads(json))
    } else {
      logger.warn(s"Error invoking call: ${contentAsString(result)}")
      getData(reads.reads(JsObject(Seq())))
    }
  }

}

trait JsonToModel {
  def getData[A](maybeData: JsResult[A]): A = maybeData match {
    case JsSuccess(d, _) => d
    case _ => throw new RuntimeException("couldn't read json")
  }
}
