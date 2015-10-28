package org.corespring.services.salat.registration

import org.bson.types.ObjectId
import org.corespring.models.assessment._
import org.corespring.models.item.{ Item, TaskInfo }
import org.corespring.models.registration.RegistrationToken
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.BeforeAfter

class RegistrationTokenServiceTest extends ServicesSalatIntegrationTest {

  "RegistrationTokenService" should {

    trait scope extends BeforeAfter with InsertionHelper {

      val service = services.registrationTokenService

      val token = mkToken("1")

      def mkToken(uid:String, expired:Boolean = false) = {
        RegistrationToken(
          uuid = uid,
          email = "user@example.com",
          creationTime = Some(DateTime.now()),
          expirationTime = Some( DateTime.now().plusDays(if(expired) -1 else 1) ),
          isSignUp = false
        )
      }

      override def before = {
        service.createToken(token)
      }

      override def after = {
        removeAllData()
      }
    }

    "createToken" should {
      "insert token into db" in new scope {
        val tokenTwo = mkToken("2")
        service.createToken(tokenTwo)
        service.findTokenByUuid(tokenTwo.uuid) must_== Some(tokenTwo)
      }
    }
    "findTokenByUuid" should {
      "return the token with uuid" in new scope {
        service.findTokenByUuid(token.uuid) must_== Some(token)
      }
      "return none when token does not exist" in new scope {
        service.findTokenByUuid("not a uuid") must_== None
      }
    }
    "deleteToken" should {
      "remove token" in new scope {
        service.deleteToken(token.uuid)
        service.findTokenByUuid(token.uuid) must_== None
      }
      "not fail if token does not exist" in new scope {
        service.deleteToken("not a uuid")
      }
    }
    "deleteExpiredTokens" should {
      "remove expired tokens" in new scope {
        val expiredTokenOne  = mkToken("expired-1", expired=true)
        val expiredTokenTwo  = mkToken("expired-2", expired=true)
        service.createToken(expiredTokenOne)
        service.createToken(expiredTokenTwo)
        service.findTokenByUuid(expiredTokenOne.uuid) must_!= None
        service.findTokenByUuid(expiredTokenTwo.uuid) must_!= None
        service.deleteExpiredTokens()
        service.findTokenByUuid(expiredTokenOne.uuid) must_== None
        service.findTokenByUuid(expiredTokenTwo.uuid) must_== None
      }
      "not remove unexpired tokens" in new scope {
        val expiredToken  = mkToken("expired", expired=true)
        val notExpiredToken  = mkToken("not-expired", expired=false)
        service.createToken(expiredToken)
        service.createToken(notExpiredToken)
        service.findTokenByUuid(expiredToken.uuid) must_!= None
        service.findTokenByUuid(notExpiredToken.uuid) must_!= None
        service.deleteExpiredTokens()
        service.findTokenByUuid(expiredToken.uuid) must_== None
        service.findTokenByUuid(notExpiredToken.uuid) must_!= None
      }
    }

  }
}

