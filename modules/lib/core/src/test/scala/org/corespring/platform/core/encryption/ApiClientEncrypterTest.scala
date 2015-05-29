package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Logger

class ApiClientEncrypterTest extends Specification with Mockito {

  PlaySingleton.start()

  val encrypter = mock[Crypto]
  val apiClientEncrypter = new ApiClientEncrypter(encrypter)

  val apiClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "secret")

  val data = "this is the string which should be encrypted"
  val encryptedData = "guvf vf gur fgevat juvpu fubhyq or rapelcgrq"

  "encrypt" should {

    encrypter.encrypt(data, apiClient.clientSecret).returns(encryptedData)

    val result = apiClientEncrypter.encrypt(apiClient, data)
      .getOrElse(throw new Exception("Expected encrypt to return Some[EncryptionResult], but got None"))

    "call encrypter#encrypt with string and client secret" in {
      there was one(encrypter).encrypt(data, apiClient.clientSecret)
    }

    "result" should {

      "be EncryptionSuccess" in {
        result must haveClass[EncryptionSuccess]
      }

      "contain apiClient clientId" in {
        result match {
          case success: EncryptionSuccess => success.clientId must be equalTo(apiClient.clientId.toString)
          case _ => failure("Encryption failed")
        }
      }

      "contain encrypted data" in {
        result match {
          case success: EncryptionSuccess => success.data must be equalTo(encryptedData)
          case _ => failure("Encryption failed")
        }
      }

      "contain unencrypted data" in {
        result match {
          case success: EncryptionSuccess => success.requested must be equalTo(Some(data))
          case _ => failure("Encryption failed")
        }
      }

    }

  }

  "decrypt" should {

    encrypter.decrypt(encryptedData, apiClient.clientSecret).returns(data)

    val result = apiClientEncrypter.decrypt(apiClient, encryptedData)
    val badClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "secret")

    "call encrypter#decrypt with encrypted data and client secret" in {
      there was atLeastOne(encrypter).decrypt(encryptedData, apiClient.clientSecret)
    }

    "result" should {

      "be decrypted data" in {
        result must be equalTo(Some(data))
      }

    }

    "with invalid data" should {

      val errorMessage = "You have bad padding, or the wrong secret, or something."

      encrypter.decrypt(encryptedData, badClient.clientSecret).throws(new RuntimeException(errorMessage))
      val result = apiClientEncrypter.decrypt(badClient, encryptedData)

      "return None" in {
        result must be equalTo(None)
      }

    }

  }

}
