package org.corespring.it.helpers

import global.Global.main
import org.bson.types.ObjectId
import org.corespring.services.auth.UpdateAccessTokenService
import org.joda.time.DateTime
import play.api.Logger

object AccessTokenHelper {

  lazy val logger = Logger(AccessTokenHelper.getClass)
  lazy val service = main.tokenService

  def expire(tokenId: String) = {
    val token = main.tokenService.findByTokenId(tokenId).get
    val update = token.copy(expirationDate = DateTime.now.minusDays(1))

    logger.info(s"function=expire, update=$update")

    main.tokenService.asInstanceOf[UpdateAccessTokenService].update(update)
  }

  def create(organizationId: ObjectId): String = {
    val client = main.apiClientService.getOrCreateForOrg(organizationId).toOption.get
    val token = service.createToken(client).toOption.get
    token.tokenId
  }

  def delete(tokenId: String) = service.removeToken(tokenId)

}
