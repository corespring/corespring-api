package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.v2.auth.WithOrgTransformer
import org.corespring.v2player.integration.actionBuilders.access.{PlayerOptions, V2PlayerCookieReader}
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

trait LoadOrgAndOptions extends UserSession with V2PlayerCookieReader {

  protected lazy val logger = LoggerFactory.getLogger("v2Player.org-id-options")

  type OrgAndOpts = (ObjectId, PlayerOptions)
  type RequestToOrgAndOpts = RequestHeader => Option[(ObjectId, PlayerOptions)]

  def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String]

  def toOrgId(apiClientId: String): Option[ObjectId]

  private def fromUserSession(r: RequestHeader): Option[OrgAndOpts] = {
    logger.trace("Try from user session")
    userFromSession(r).map(
      u => {
        logger.trace(s"found user: $u")
        (u.org.orgId, PlayerOptions.ANYTHING)
      })
  }

  private def fromCookies(r: RequestHeader): Option[OrgAndOpts] = {
    logger.trace("Try from cookies")
    for {
      orgId <- orgIdFromCookie(r)
      options <- renderOptions(r)
    } yield {
      (new ObjectId(orgId), options)
    }
  }

  private def fromQueryParams(r: RequestHeader): Option[OrgAndOpts] = {
    logger.trace("Try from query params")
    for {
      apiClientId <- r.getQueryString("apiClient")
      encryptedOptions <- r.getQueryString("options")
      orgId <- toOrgId(apiClientId)
      decryptedOptions <- decrypt(r, orgId, encryptedOptions)
      playerOptions <- PlayerOptions.fromJson(decryptedOptions)
    } yield (orgId, playerOptions)
  }

  def finders: Seq[(RequestHeader) => Option[OrgAndOpts]] = Seq(
    fromUserSession,
    fromCookies,
    fromQueryParams
  )

  def transformer : WithOrgTransformer[OrgAndOpts]

  /**
   * TODO: There seems like there is crossover her with <code>RequestTransformer</code>. Should look to centralize.
   * @param request
   * @return
   */
  def getOrgIdAndOptions(request: RequestHeader): Option[OrgAndOpts] = { transformer(request)}

  /*finders.foldLeft[Option[OrgAndOpts]](None){ (acc, fn) => acc match {
      case Some(o) => Some(o)
      case _ => fn(request)
    }
  }*/
}

