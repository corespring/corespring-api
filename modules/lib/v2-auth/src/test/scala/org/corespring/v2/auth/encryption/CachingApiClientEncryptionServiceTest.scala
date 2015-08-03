package org.corespring.v2.auth.encryption

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ EncryptionSuccess, ApiClientEncryptionService }
import org.corespring.models.auth.ApiClient
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.duration.Duration

class CachingApiClientEncryptionServiceTest extends Specification with Mockito {
  val apiClient = ApiClient(orgId = new ObjectId(), clientId = new ObjectId(), clientSecret = "secret")
  val data = "this is my data"
  val encryptedData = "guvf vf zl qngn"
  val encryptionSuccess = EncryptionSuccess(apiClient.clientId.toString, encryptedData, Some(data))
  val orgId = new ObjectId()

  trait CachedEncryptionService extends Scope {
    val apiClientEncryptionService = mock[ApiClientEncryptionService]
    apiClientEncryptionService.decrypt(apiClient.clientId.toString, encryptedData).returns(Some(data))
    apiClientEncryptionService.encrypt(apiClient.clientId.toString, data).returns(Some(encryptionSuccess))
    apiClientEncryptionService.encrypt(apiClient, data).returns(Some(encryptionSuccess))
    apiClientEncryptionService.encryptByOrg(orgId, data).returns(Some(encryptionSuccess))
    val cachedEncryptionService = new CachingApiClientEncryptionService(apiClientEncryptionService, Duration.Inf)
  }

  "encrypt" should {

    "with apiClientId" should {

      "delegate to underlying encrypter" in new CachedEncryptionService {
        val result = cachedEncryptionService.encrypt(apiClient.clientId.toString, data)
        result must be equalTo (Some(encryptionSuccess))
        there was one(apiClientEncryptionService).encrypt(apiClient.clientId.toString, data)
      }

    }

    "with apiClient" should {

      "delegate to underlying encrypter" in new CachedEncryptionService {
        val result = cachedEncryptionService.encrypt(apiClient, data)
        result must be equalTo (Some(encryptionSuccess))
        there was one(apiClientEncryptionService).encrypt(apiClient, data)
      }

    }

  }

  "encryptByOrg" should {

    "delegate to underlying encrypter" in new CachedEncryptionService {
      val result = cachedEncryptionService.encryptByOrg(orgId, data)
      result must be equalTo (Some(encryptionSuccess))
      there was one(apiClientEncryptionService).encryptByOrg(orgId, data)
    }

  }

  "decrypt" should {

    "called twice" should {

      "not delegate to underlying encrypter on second call" in new CachedEncryptionService {
        cachedEncryptionService.decrypt(apiClient, encryptedData)
        cachedEncryptionService.decrypt(apiClient, encryptedData)
        there was one(apiClientEncryptionService).decrypt(apiClient.clientId.toString, encryptedData)
      }

    }

  }

  "flush" should {

    "delegate to underlying encrypter after called" in new CachedEncryptionService {
      cachedEncryptionService.decrypt(apiClient, encryptedData)
      cachedEncryptionService.flush
      cachedEncryptionService.decrypt(apiClient, encryptedData)
      there were two(apiClientEncryptionService).decrypt(apiClient.clientId.toString, encryptedData)
    }

  }

}
