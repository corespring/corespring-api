import org.specs2.mutable.Specification
import models.{Item, ItemResponse, ItemSession}
import org.bson.types.ObjectId
import org.specs2.execute.Pending
import play.api.libs.json.Json
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.mvc.{Results, SimpleResult, AnyContentAsJson}
import play.api.test.{FakeHeaders, FakeRequest}
import org.specs2.mutable._
import play.api.test.Helpers._

object FieldValuesApiTest extends Specification {

  PlaySingleton.start()

  "FieldValuesApi" should {
    "show all available values" in {

      val request = FakeRequest(GET, "/api/v1/field_values")
      val maybeResult = routeAndCall(request)

      if (!maybeResult.isDefined) {
        failure
      }
      val result = maybeResult.get.asInstanceOf[SimpleResult[AnyContentAsJson]]
      result.header.status.mustEqual(200)
      val json: JsValue = Json.parse(contentAsString(result))
      val array: JsArray = json.asInstanceOf[JsArray]
      array.value.length.mustEqual(10)


      //iterate through each path and ensure its a 200
      for ( jso <- array.value ){
        val path = (jso \ "path").asOpt[String]
        val subRequest = FakeRequest( GET, path.get.toString )
        val maybeSubResult = routeAndCall(subRequest)
        if ( !maybeSubResult.isDefined ){
          failure
        }

        val subResult = maybeSubResult.get.asInstanceOf[SimpleResult[AnyContentAsJson]]
        subResult.header.status.mustEqual(200)
      }

      true.mustEqual(true)
    }

  }

}
