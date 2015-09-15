package org.corespring.models.json

import org.corespring.models.registration.RegistrationToken
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class RegistrationTokenFormatTest extends Specification{

  import RegistrationTokenFormat.format

  "writes" should {

    "write a token to json" in {
      val token = RegistrationToken(
        uuid = "uuid",
        email = "email",
        creationTime = Some(new DateTime()),
        expirationTime = Some(new DateTime() plusHours 1),
        isSignUp = true)

      val json = Json.toJson(token)

      (json \ RegistrationToken.Keys.Uuid).asOpt[String] must equalTo(Some("uuid"))
      (json \ RegistrationToken.Keys.Email).asOpt[String] must equalTo(Some("email"))
      (json \ RegistrationToken.Keys.Created).as[Long] must equalTo(token.creationTime.get.getMillis)
      (json \ RegistrationToken.Keys.IsSignUp).as[Boolean] must equalTo(true)
    }
  }

  "reads" should {
    "read json" in {
      val token = RegistrationToken(
        uuid = "uuid",
        email = "email",
        creationTime = Some(new DateTime()),
        expirationTime = Some(new DateTime() plusHours 1),
        isSignUp = true)

      val json = Json.toJson(token)
      val parsed = json.as[RegistrationToken]

      parsed === token
    }
  }

}
