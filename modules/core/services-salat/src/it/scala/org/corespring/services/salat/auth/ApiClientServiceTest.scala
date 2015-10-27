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

  "findByIdAndSecret" should {
    "work" in pending
  }
  "findByKey" should {
    "work" in pending
  }
  "findOneByOrgId" should {
    "work" in pending
  }
  "generateTokenId" should {
    "generate a 25 character token id" in new scope {
      //TODO Why are we using this strange code to generate a token id?
      //Not sure, what the requirements are
      //I think it uses a lot of memory and also the number of primes is limited
      //Wouldn't a simple id like current Date plus random part be better?
      //Or the mongo object id ?
      val res = service.generateTokenId()
      res.length must beEqualTo(25)
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

