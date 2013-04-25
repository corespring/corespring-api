package tests.api.v1

import tests.BaseTest
import play.api.test._
import models.auth.{ApiClient, AccessToken}
import play.api.test.Helpers._
import play.api.libs.json.{Json}
import encryption.AESCrypto
import controllers.auth.{RenderOptions}
import play.api.mvc.Call
import play.api.test.FakeHeaders
import play.api.mvc.AnyContentAsJson
import scala.Some


class RenderKeyTest extends BaseTest{
  val update:Call = player.controllers.routes.RenderKey.encrypt()
  val renderOptions = RenderOptions("50083ba9e4b071cb5ef79101","502d0f823004deb7f4f53be7","*","*","student",0,"render")
  val fakeRequest = FakeRequest(update.method,tokenize(update.url),FakeHeaders(),AnyContentAsJson(Json.toJson(renderOptions)))
  val Some(result) = routeAndCall(fakeRequest)
  status(result) must equalTo(OK)
  charset(result) must beSome("utf-8")
  contentType(result) must beSome("application/json")
  val apiClient = ApiClient.findOneByOrgId(AccessToken.findById(token).get.organization).get
  var clientId:Option[String] = None
  var encrypted:Option[String] = None
  "registering a key with render constraints" should {
    "return a clientId and options" in {
      val jsresult = Json.parse(contentAsString(result))
      clientId = (jsresult \ "clientId").asOpt[String]
      encrypted = (jsresult \ "options").asOpt[String]
      clientId must beSome[String]
      encrypted must beSome[String]
    }
//    "return a session with client id and options equivalent to the json received" in {
//      val resultsession:Session = session(result)
//      val (sessionclientId,sessionencrypted) = resultsession.get(BaseRender.RendererHeader) match {
//        case Some(strctx) => strctx.split(BaseRender.Delimeter) match {
//          case Array(sessionclientId,sessionencrypted) => (Some(sessionclientId),Some(sessionencrypted))
//          case _ => (None,None)
//        }
//        case None => (None,None)
//      }
//      sessionclientId must beEqualTo(clientId)
//      sessionencrypted must beEqualTo(encrypted)
//    }
    "return a key with the correct client id" in {
      clientId must beSome(apiClient.clientId.toString)
    }
    "return a key that contains encrypted options that can be decrypted using the client secret to equal the constraints sent" in {
      val decryptedOptions = encrypted.map(AESCrypto.decrypt(_,apiClient.clientSecret))
      val receivedOptions = decryptedOptions.map(options => Json.fromJson[RenderOptions](Json.parse(options)))
      receivedOptions must beSome(renderOptions)
    }
  }
}
