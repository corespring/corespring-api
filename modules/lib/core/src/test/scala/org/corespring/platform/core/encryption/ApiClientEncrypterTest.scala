package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.test.PlaySingleton
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Logger

class ApiClientEncrypterTest extends Specification with Mockito {

  PlaySingleton.start()

  trait WithEncrypter extends Scope {

    val orgId = new ObjectId()

    val apiClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "secret")
    val badClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "badSecret")
    val randomApiClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "randomSecret")

    val errorMessage = "You have bad padding, or the wrong secret, or something."
    val data = "this is the string which should be encrypted"
    val encryptedData = "guvf vf gur fgevat juvpu fubhyq or rapelcgrq"

    val encrypter = mock[Crypto]
    val apiClientEncrypter = new ApiClientEncrypter(encrypter) {
      override def randomApiClientForOrg(orgId: ObjectId) = Some(randomApiClient)
    }

    encrypter.encrypt(data, apiClient.clientSecret).returns(encryptedData)
    encrypter.encrypt(data, randomApiClient.clientSecret).returns(encryptedData)
    encrypter.decrypt(encryptedData, apiClient.clientSecret).returns(data)
    encrypter.decrypt(encryptedData, badClient.clientSecret).throws(new RuntimeException(errorMessage))
  }

  "encrypt" should {

    "call encrypter#encrypt with string and client secret" in new WithEncrypter {
      apiClientEncrypter.encrypt(apiClient, data)
      there was one(encrypter).encrypt(data, apiClient.clientSecret)
    }

    "result" should {

      "be EncryptionSuccess" in new WithEncrypter {
        val encryptResult = apiClientEncrypter.encrypt(apiClient, data)
          .getOrElse(throw new Exception("Expected encrypt to return Some[EncryptionResult], but got None"))
        encryptResult must haveClass[EncryptionSuccess]
      }

      "contain apiClient clientId" in new WithEncrypter {
        val encryptResult = apiClientEncrypter.encrypt(apiClient, data)
          .getOrElse(throw new Exception("Expected encrypt to return Some[EncryptionResult], but got None"))
        encryptResult match {
          case success: EncryptionSuccess => success.clientId must be equalTo(apiClient.clientId.toString)
          case _ => failure("Encryption failed")
        }
      }

      "contain encrypted data" in new WithEncrypter {
        val encryptResult = apiClientEncrypter.encrypt(apiClient, data)
          .getOrElse(throw new Exception("Expected encrypt to return Some[EncryptionResult], but got None"))
        encryptResult match {
          case success: EncryptionSuccess => success.data must be equalTo(encryptedData)
          case _ => failure("Encryption failed")
        }
      }

      "contain unencrypted data" in new WithEncrypter {
        val encryptResult = apiClientEncrypter.encrypt(apiClient, data)
          .getOrElse(throw new Exception("Expected encrypt to return Some[EncryptionResult], but got None"))
        encryptResult match {
          case success: EncryptionSuccess => success.requested must be equalTo(Some(data))
          case _ => failure("Encryption failed")
        }
      }

    }

  }

  "encryptByOrg" should {

    "call apiClientEncrypter.encrypt with random api client secret" in new WithEncrypter {
      apiClientEncrypter.encryptByOrg(orgId, data)
      there was one(apiClientEncrypter).encrypt(encryptedData, randomApiClient.clientSecret)
    }

  }

  "decrypt" should {

    "call encrypter#decrypt with encrypted data and client secret" in new WithEncrypter {
      apiClientEncrypter.decrypt(apiClient, encryptedData)
      there was one(encrypter).decrypt(encryptedData, apiClient.clientSecret)
    }

    "result" should {

      "be decrypted data" in new WithEncrypter {
        apiClientEncrypter.decrypt(apiClient, encryptedData) must be equalTo(Some(data))
      }

    }

    "with invalid data" should {

      "return None" in new WithEncrypter {
        apiClientEncrypter.decrypt(badClient, encryptedData) must be equalTo(None)
      }

    }

  }

}
