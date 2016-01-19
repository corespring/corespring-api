package org.corespring.models.auth

import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * //TODO: creationDate, expirationDate and neverExpire could be reduced to 2 properties: creationDate, Option(duration) - if None => neverExpire
 */
case class AccessToken(
  //TODO: AC-258 - rm Option once all unexpired access tokens have the apiClientId.
  apiClientId: Option[ObjectId] = None,
  organization: ObjectId,
  scope: Option[String],
  tokenId: String,
  creationDate: DateTime = DateTime.now(),
  expirationDate: DateTime = DateTime.now().plusHours(24),
  neverExpire: Boolean = false) {
  def isExpired: Boolean = {
    !neverExpire && DateTime.now().isAfter(expirationDate)
  }

  override def toString = {
    s"AccessToken(tokenId: $tokenId, apiClientId: $apiClientId, org: $organization, isExpired: $isExpired, expiration: $expirationDate)"
  }
}

