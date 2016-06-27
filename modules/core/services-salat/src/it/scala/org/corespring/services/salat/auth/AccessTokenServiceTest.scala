package org.corespring.services.salat.auth

import com.mongodb.casbah.WriteConcern
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.auth.AccessToken
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.BeforeAfter

import scalaz.Success

class AccessTokenServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter with InsertionHelper {

    val service = services.tokenService

    val org = insertOrg("1")
    val apiClient = services.apiClientService.getOrCreateForOrg(org.id).toOption.get
    val token = service.createToken(apiClient).toOption

    override def before = {

    }

    override def after = {
      removeAllData()
    }

    def isDate(d1: DateTime)(d2: DateTime) = {
      Math.abs(d1.getMillis - d2.getMillis) < 1000
    }

    def updateToken(token: AccessToken) = {
      services.tokenDao.update(
        MongoDBObject("tokenId" -> token.tokenId),
        token, false, false, WriteConcern.Safe)
    }
  }

  "createToken" should {

    "return token" in new scope {
      token.isDefined must_== true
    }
    "set the org of the token to the org id" in new scope {
      token.map(_.organization) must_== Some(org.id)
    }
    "set the creationDate to now" in new scope {
      token.map(_.creationDate).map(isDate(DateTime.now)) must_== Some(true)
    }
    "set the expirationDate to now + 24H" in new scope {
      token.map(_.expirationDate).map(isDate(DateTime.now.plusHours(24))) must_== Some(true)
    }
    "set the scope to None" in new scope {
      token.map(_.scope) must_== Some(None)
    }
  }

  "findByTokenId" should {
    "return the token for an id" in new scope {
      service.findByTokenId(token.get.tokenId) must_== token
    }
    "return None, if token does not exist" in new scope {
      service.findByTokenId(ObjectId.get.toString) must_== None
    }
  }

  "orgForToken" should {

    "return the org if the token exists and is not expired" in new scope {
      service.orgForToken(token.get.tokenId) must_== Success(org)
    }

    "return failure if the token does not exist" in new scope {
      service.orgForToken("some non existent id").isFailure must_== true
    }

    "return failure if the token is expired" in new scope {
      val updated = token.get.copy(expirationDate = DateTime.now.minusHours(2))
      updateToken(updated)
      service.orgForToken(updated.tokenId).isFailure must_== true
    }

    "return failure if the org does not exist" in new scope {
      val updated = token.get.copy(organization = ObjectId.get)
      updateToken(updated)
      service.orgForToken(updated.tokenId).isFailure must_== true
    }
  }

  "removeToken" should {
    "remove an existing token" in new scope {
      service.removeToken(token.get.tokenId)
      service.findByTokenId(token.get.tokenId) must_== None
    }
    "not fail if the token does not exist" in new scope {
      service.removeToken("non existent token")
    }
  }

}

