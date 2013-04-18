package tests.api.v1

import tests.BaseTest
import play.api.mvc.{AnyContentAsJson, Call}
import play.api.test._
import models.auth.{ApiClient, AccessToken}
import org.bson.types.ObjectId
import play.api.test.Helpers._
import play.api.mvc.Call
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Some
import play.api.libs.json.{JsValue, Json}
import controllers.auth.{RenderConstraints, AESCrypto, RendererContext}


class RenderApiTest extends BaseTest{
  val update:Call = api.v1.routes.RenderApi.getRenderKey()
  val renderConstraints = RenderConstraints(Some("50083ba9e4b071cb5ef79101"),Some("502d0f823004deb7f4f53be7"),None,None,0)
  val fakeRequest = FakeRequest(update.method,tokenize(update.url),FakeHeaders(),AnyContentAsJson(Json.toJson(renderConstraints)))
  val Some(result) = routeAndCall(fakeRequest)
  status(result) must equalTo(OK)
  charset(result) must beSome("utf-8")
  contentType(result) must beSome("application/json")
  val apiClient = ApiClient.findOneByOrgId(AccessToken.findById(token).get.organization).get
  var key = ""
  var parts = Array[String]()
  "registering a key with render constraints" should {
    "return a key" in {
      (Json.parse(contentAsString(result)) \ "key").asOpt[String] match {
        case Some(k) => {
          key = k;
          success;
        }
        case None => failure
      }
    }
    "return a key that can be split into clientId and render constraints" in {
      parts = key.split(RendererContext.keyDelimeter)
      parts.length must beEqualTo(2)
    }
    "return a key with the correct client id" in {
      parts(0) must beEqualTo(apiClient.clientId.toString)
    }
    "return a key that contains encrypted constraints that can be decrypted using the client secret to equal the constraints sent" in {
      val decryptedConstraints = AESCrypto.decryptAES(parts(1),apiClient.clientSecret)
      val receivedConstraints = Json.fromJson[RenderConstraints](Json.parse(decryptedConstraints))
      RenderConstraints(receivedConstraints.itemId,receivedConstraints.itemSessionId,receivedConstraints.assessmentId,None,receivedConstraints.expires) must beEqualTo(renderConstraints)
    }
  }
}
