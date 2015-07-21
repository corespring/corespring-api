package org.corespring.models.auth

import org.bson.types.ObjectId
import org.joda.time.DateTime

/**
 * An access token
 *
 * @see ApiClient
 */

case class AccessToken(organization: ObjectId,
    scope: Option[String],
    tokenId: String,
    creationDate: DateTime = DateTime.now(),
    expirationDate: DateTime,
    neverExpire: Boolean = false) {
  def isExpired: Boolean = {
    !neverExpire && DateTime.now().isAfter(expirationDate)
  }
}

