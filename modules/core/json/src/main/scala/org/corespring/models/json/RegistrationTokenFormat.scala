package org.corespring.models.json

import org.corespring.models.registration.RegistrationToken
import play.api.libs.json.{Json}

object RegistrationTokenFormat{

  implicit val oidFormat = ObjectIdFormat
  implicit val format = Json.format[RegistrationToken]

}
