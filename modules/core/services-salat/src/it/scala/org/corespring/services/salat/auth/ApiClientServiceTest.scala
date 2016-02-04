package org.corespring.services.salat.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.{ ApiClient, AccessToken }
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.BeforeAfter

import scalaz.{ Validation, Success }

class ApiClientServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter with InsertionHelper {

    val service = services.apiClientService
    val org = insertOrg("1")

    override def before = {

    }

    override def after = {
      removeAllData()
    }

  }

  "findByClientIdAndSecret" should {
    trait findByClientIdAndSecretScope extends scope {
      val apiClient = service.getOrCreateForOrg(org.id).toOption.get
    }
    "find apiClient by client id and secret" in new findByClientIdAndSecretScope {
      service.findByClientIdAndSecret(apiClient.clientId.toString, apiClient.clientSecret) must_== Some(apiClient)
    }
    "throw if client id is not an object id" in new findByClientIdAndSecretScope {
      service.findByClientIdAndSecret("not a client id", apiClient.clientSecret) must throwA[IllegalArgumentException]
    }
    "return none if client id is not valid" in new findByClientIdAndSecretScope {
      service.findByClientIdAndSecret(ObjectId.get.toString, apiClient.clientSecret) must_== None
    }
    "return none if secret is not valid" in new findByClientIdAndSecretScope {
      service.findByClientIdAndSecret(apiClient.clientId.toString, "not a secret") must_== None
    }
    "return none if both are not valid" in new findByClientIdAndSecretScope {
      service.findByClientIdAndSecret(ObjectId.get.toString, "not a secret") must_== None
    }
  }

  "findByClientId" should {
    trait findByClientIdScope extends scope {
      val apiClient = service.getOrCreateForOrg(org.id).toOption.get
    }
    "find apiClient by clientId" in new findByClientIdScope {
      service.findByClientId(apiClient.clientId.toString) must_== Some(apiClient)
    }
    "throw if client id is not an object id" in new findByClientIdScope {
      service.findByClientId("not a client id") must throwA[IllegalArgumentException]
    }
    "return none when clientId does not match any api client" in new findByClientIdScope {
      service.findByClientId(ObjectId.get.toString) must_== None
    }
  }

  "findByOrgId" should {
    "find an existing api client by org id" in new scope {
      val apiClient = service.getOrCreateForOrg(org.id).toOption
      service.findByOrgId(org.id) must_== apiClient.toSeq
    }
    "return none if api client does not exist" in new scope {
      service.findByOrgId(org.id) must_== Nil
    }
    "return none if org does not exist" in new scope {
      service.findByOrgId(ObjectId.get) must_== Nil
    }
  }

  "getOrCreateForOrg" should {
    "create a new apiClient, if org does not have one" in new scope {
      service.getOrCreateForOrg(org.id).isSuccess must_== true
    }
    "return an existing apiClient, if org does have one" in new scope {
      val apiClient = service.getOrCreateForOrg(org.id)
      service.getOrCreateForOrg(org.id) must_== apiClient
    }
    "fail when org does not exist" in new scope {
      service.getOrCreateForOrg(ObjectId.get).isFailure must_== true
    }

  }

}

