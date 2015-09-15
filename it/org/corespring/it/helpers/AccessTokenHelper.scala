package org.corespring.it.helpers

import bootstrap.Main
import org.bson.types.ObjectId

object AccessTokenHelper {

  lazy val service = Main.tokenService

  def create(organizationId: ObjectId, userName: String): String = {
    val token = service.getOrCreateToken(organizationId)
    token.tokenId
  }

  def delete(tokenId: String) = service.removeToken(tokenId)

}
