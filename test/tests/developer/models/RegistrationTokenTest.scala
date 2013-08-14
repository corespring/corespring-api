package tests.developer.models

import org.specs2.mutable.Specification
import org.corespring.platform.core.models._
import play.api.libs.json.Json
import scala.Some
import org.joda.time.DateTime
import developer.models.RegistrationToken
import org.corespring.test.BaseTest

class RegistrationTokenTest extends BaseTest {
  "token" should {

    "serializing to json" in {
      val token = RegistrationToken(
        uuid = "uuid",
        email = "email",
        creationTime = Some(new DateTime()),
        expirationTime = Some(new DateTime() plusHours 1),
        isSignUp = true
      )

      val json = Json.toJson(token)

      (json \ RegistrationToken.Uuid).asOpt[String] must equalTo(Some("uuid"))
      (json \ RegistrationToken.Email).asOpt[String] must equalTo(Some("email"))
      (json \ RegistrationToken.Created).as[Long] must equalTo(token.creationTime.get.getMillis)
      (json \ RegistrationToken.IsSignUp).as[Boolean] must equalTo(true)
    }

    "deserializing from json" in {
      val token = RegistrationToken(
              uuid = "uuid",
              email = "email",
              creationTime = Some(new DateTime()),
              expirationTime = Some(new DateTime() plusHours 1),
              isSignUp = true
            )

      val json = Json.toJson(token)
      val parsed = json.as[RegistrationToken]

      parsed.uuid must equalTo(token.uuid)
      parsed.email must equalTo(token.email)
      parsed.creationTime must equalTo(token.creationTime)
      parsed.expirationTime must equalTo(token.expirationTime)
      parsed.isSignUp must equalTo(token.isSignUp)
    }
  }
}
