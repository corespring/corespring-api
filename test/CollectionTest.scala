import controllers.auth.Permission
import controllers.Log
import models.ContentCollection
import org.bson.types.ObjectId
import org.specs2.mutable._
import play.api.libs.json.{JsNull, Json, JsValue}
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import play.api.test._


class CollectionTest extends Specification {

    PlaySingleton.start()

    val token = "34dj45a769j4e1c0h4wb"
    val request = FakeRequest(GET, "/api/v1/collections?access_token="+token)//.withHeaders(("Authorization","Bearer "+token))
    val optResult = routeAndCall(request)
    val json:JsValue = if (optResult.isDefined) Json.parse(contentAsString(optResult.get)) else JsNull
    Log.i(json.toString())
    // TODO check the value and verify it is as expected
    pending

}
