package org.corespring.services.salat.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.AccessToken
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.BeforeAfter
import org.specs2.specification.Outside

import scalaz.Success

class AccessTokenServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter with InsertionHelper {

    val service = services.tokenService

    val org = insertOrg("1")
    val apiClient = services.apiClientService.getOrCreateForOrg(org.id).toOption.get
    val token = service.createToken(apiClient.clientId.toString, apiClient.clientSecret).toOption

    override def before = {

    }

    override def after = {
      removeAllData()
    }

    def isDate(d1: DateTime)(d2: DateTime) = {
      Math.abs(d1.getMillis - d2.getMillis) < 1000
    }

    def mkToken(
      neverExpire: Option[Boolean] = None,
      creationDate: Option[DateTime] = None,
      expirationDate: Option[DateTime] = None) = {
      val token = AccessToken(
        ObjectId.get,
        None,
        ObjectId.get.toString,
        neverExpire = neverExpire.getOrElse(false),
        creationDate = creationDate.getOrElse(DateTime.now),
        expirationDate = expirationDate.getOrElse(DateTime.now.plusHours(24)))
      service.insertToken(token)
      token
    }

    def mkExpiredToken() = {
      val now = DateTime.now
      mkToken(creationDate=Some(now), expirationDate=Some(now.minusDays(10)))
    }
    def mkNeverExpiringToken() = {
      mkToken(neverExpire=Some(true))
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
  "find" should {
    "return token for org" in new scope {
      service.find(org.id, None) must_== token
    }
    "return token for org and scope" in new scope {
      //TODO How to add scope to access token?
      //service.find(org.id, Some("test-scope")) must_== token
    }
    "return None when org is not correct" in new scope {
      service.find(ObjectId.get, None) must_== None
    }
    "return None when scope is not correct" in new scope {
      service.find(org.id, Some("test-scope")) must_== None
    }

  }
  "findById" should {
    //@deprecated
    "work" in pending
  }
  "findByOrgId" should {
    "return the token if it is not expired" in new scope {
      service.findByOrgId(org.id) must_== token
    }
    "create a new token if it is expired" in new scope {
      //TODO Create? Really? Maybe a name change would be good to signal that
      val expiredToken = mkExpiredToken()
      val res = service.findByOrgId(expiredToken.organization)
      res.isDefined must_== true
      res must_!= expiredToken

    }
    "create a new token if it does not exist" in new scope {
      //TODO Create? Really? Maybe a name change would be good to signal that
      val orgTwo = insertOrg("2")
      service.findByOrgId(orgTwo.id).isDefined must_== true
    }
  }
  "findByToken" should {
    //@deprecated
    "work" in pending
  }
  "findByTokenId" should {
    "return the token for an id" in new scope {
      service.findByTokenId(token.get.tokenId) must_== token
    }
    "return None, if token does not exist" in new scope {
      service.findByTokenId(ObjectId.get.toString) must_== None
    }
  }

  "getOrCreateToken" should {
    "return token for org id" in new scope {
      service.getOrCreateToken(org.id) must_== token.get
    }
    "return token for org" in new scope {
      service.getOrCreateToken(org) must_== token.get
    }
    "return new token, when token is expired" in new scope {
      val expiredToken = mkExpiredToken()
      val res = service.getOrCreateToken(expiredToken.organization)
      res must haveClass[AccessToken]
      res must_!= expiredToken
    }
    "return new token, when token does not exist" in new scope {
      val orgTwo = insertOrg("2")
      service.getOrCreateToken(orgTwo) must haveClass[AccessToken]
    }
  }

  "getTokenForOrgById" should {
    //@deprecated
    "work" in pending
  }
  "insertToken" should {
    "insert a token as is" in new scope {
      val t = mkToken()
      service.findByTokenId(t.tokenId) must_== Some(t)
    }
    "allow to insert a token with neverExpire = true" in new scope {
      val t = mkNeverExpiringToken()
      service.findByTokenId(t.tokenId) must_== Some(t)
    }
    "allow to insert an expired token" in new scope {
      val expiredToken = mkExpiredToken()
      service.findByTokenId(expiredToken.tokenId) must_== Some(expiredToken)
    }
    "not change an existing token" in new scope {
      val now = DateTime.now
      token.get.neverExpire must_== false
      val t = token.get.copy(neverExpire = true)
      service.findByTokenId(t.tokenId) must_== token
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
      val t = mkExpiredToken()
      service.orgForToken(t.tokenId).isFailure must_== true
    }
    "return failure if the org does not exist" in new scope {
      val t = mkToken()
      service.orgForToken(t.tokenId).isFailure must_== true
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

