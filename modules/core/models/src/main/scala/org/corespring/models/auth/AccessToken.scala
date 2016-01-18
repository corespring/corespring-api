package org.corespring.models.auth

import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * //TODO: creationDate, expirationDate and neverExpire could be reduced to 2 properties: creationDate, Option(duration) - if None => neverExpire
 */
case class AccessToken(organization: ObjectId,
  scope: Option[String],
  tokenId: String,
  apiClientId: ObjectId,
  creationDate: DateTime = DateTime.now(),
  expirationDate: DateTime = DateTime.now().plusHours(24),
  neverExpire: Boolean = false) {
  def isExpired: Boolean = {
    !neverExpire && DateTime.now().isAfter(expirationDate)
  }
}

