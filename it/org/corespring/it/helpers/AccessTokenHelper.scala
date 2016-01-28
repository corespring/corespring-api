package org.corespring.it.helpers

import global.Global.main
import org.bson.types.ObjectId

object AccessTokenHelper {

  lazy val service = main.tokenService

  def create(organizationId: ObjectId): String = {
    val client = main.apiClientService.getOrCreateForOrg(organizationId).toOption.get
    val token = service.createToken(client).toOption.get
    token.tokenId
  }

  def delete(tokenId: String) = service.removeToken(tokenId)

}
