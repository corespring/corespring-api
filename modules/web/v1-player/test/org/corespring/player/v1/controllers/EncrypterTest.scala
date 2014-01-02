package org.corespring.player.v1.controllers

import org.corespring.common.encryption.{Crypto, AESCrypto}
import org.corespring.platform.core.models.auth.{ApiClient, AccessToken}
import org.corespring.player.accessControl.models.{RequestedAccess, RenderOptions}
import org.corespring.test.BaseTest
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, AnyContentAsJson}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

class EncrypterTest extends BaseTest {


  "encrypter" should {

    val action = Encrypter.encryptOptions()
    val renderOptions = RenderOptions("50083ba9e4b071cb5ef79101", "502d0f823004deb7f4f53be7", "*", "student", 0, RequestedAccess.Mode.Render)
    val fakeRequest = FakeRequest("ignore", tokenize("ignore"), FakeHeaders(), AnyContentAsJson(Json.toJson(renderOptions)))
    val result = action(fakeRequest)

    "call using an access token" in {
      status(result) === OK
      charset(result) === Some("utf-8")
      contentType(result) === Some("application/json")
    }

    val apiClient = ApiClient.findOneByOrgId(AccessToken.findById(token).get.organization).get
    var clientId: Option[String] = None
    var encrypted: Option[String] = None
    val jsresult = Json.parse(contentAsString(result))
    clientId = (jsresult \ "clientId").asOpt[String]
    encrypted = (jsresult \ "options").asOpt[String]

    "return a clientId and options" in {
      clientId must beSome[String]
      encrypted must beSome[String]
    }

    "return a key with the correct client id" in {
      clientId must beSome(apiClient.clientId.toString)
    }

    "return a key that contains encrypted options that can be decrypted using the client secret to equal the constraints sent" in {
      val optionsString: Option[String] = encrypted.map(AESCrypto.decrypt(_, apiClient.clientSecret))
      val decryptedOptions: Option[RenderOptions] = optionsString.map(options => (Json.parse(options)).as[RenderOptions])
      decryptedOptions === Some(renderOptions)
    }

    "gives validation errors" in {

      object MockCrypto extends Crypto {
        def encrypt(message: String, privateKey: String): String = message

        def decrypt(encrypted: String, privateKey: String): String = encrypted
      }

      val mockEncrypter = new Encrypter(MockCrypto)

      def runRequestAndReturnStatus(r: FakeRequest[AnyContent]): Int = {
        val out = mockEncrypter.encryptOptions(r)
        status(out)
      }

      def fr(c: AnyContent): FakeRequest[AnyContent] = FakeRequest("", tokenize(".."), FakeHeaders(), c)

      runRequestAndReturnStatus(fr(AnyContentAsEmpty)) === BAD_REQUEST
      runRequestAndReturnStatus(fr(AnyContentAsJson(Json.parse( """{"test": true}""")))) === OK
      runRequestAndReturnStatus(fr(AnyContentAsJson(Json.toJson(RenderOptions(expires = 0, mode = RequestedAccess.Mode.All))))) === OK

    }
  }
}
