package org.corespring.models.registration

import org.bson.types.ObjectId
import org.joda.time.DateTime

case class RegistrationToken(
  uuid: String,
  email: String,
  creationTime: Option[DateTime],
  expirationTime: Option[DateTime],
  isSignUp: Boolean,
  id: ObjectId = new ObjectId())

object RegistrationToken {

  object Keys {
    val Id = "id"
    val Uuid = "uuid"
    val Email = "email"
    val Created = "creationTime"
    val Expires = "expirationTime"
    val IsSignUp = "isSignUp"
  }
}
