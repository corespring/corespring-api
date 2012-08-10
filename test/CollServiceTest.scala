import controllers.Log
import org.specs2.mutable._
import play.api.libs.json.{JsNull, Json, JsValue}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.test._

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/9/12
 * Time: 4:33 PM
 * To change this template use File | Settings | File Templates.
 */

class CollServiceTest extends Specification {
  running(FakeApplication()){
    val token = "34dj45a769j4e1c0h4wb"
    val request = FakeRequest(GET, "/api/v1/collections").
      withHeaders(("Authorization","Bearer "+token))
    val optResult = routeAndCall(request)
    val json:JsValue = if (optResult.isDefined) Json.parse(contentAsString(optResult.get)) else JsNull
    Log.i(json.toString())
  }
}
