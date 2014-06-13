package org.corespring.v2player.integration.actionBuilders

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.v2player.integration.actionBuilders.access.{PlayerOptions, V2PlayerCookieReader}
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

trait LoadOrgAndOptions extends UserSession with V2PlayerCookieReader {

  protected lazy val logger = LoggerFactory.getLogger("v2Player.org-id-options")

  type OrgAndOpts = (ObjectId, PlayerOptions)
  type RequestToOrgAndOpts = RequestHeader => Option[(ObjectId, PlayerOptions)]

  def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String]

  def toOrgId(apiClientId: String): Option[ObjectId]

  private def fromUserSession(r: RequestHeader): Option[OrgAndOpts] = userFromSession(r).map(
    u => {
      logger.trace(s"found user: $u")
      (u.org.orgId, PlayerOptions.ANYTHING)
    })

  private def fromCookies(r: RequestHeader): Option[OrgAndOpts] = for {
    orgId <- orgIdFromCookie(r)
    options <- renderOptions(r)
  } yield {
    (new ObjectId(orgId), options)
  }

  private def fromQueryParams(r: RequestHeader): Option[OrgAndOpts] = {
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

  def getOrgIdAndOptions(request: RequestHeader): Option[(ObjectId, PlayerOptions)] = {

    finders.tail.foldRight[RequestToOrgAndOpts](finders.head){ (fn : RequestToOrgAndOpts, accFn : RequestToOrgAndOpts) =>
      (fn _) andThen (accFn _)
    }

    finders.foldRight(None)
    logger.trace(s"[getOrgIdAndOptions]: ${request.cookies}")
    userFromSession(request).map(
      u => {
        logger.trace(s"found user: $u")
        (u.org.orgId, PlayerOptions.ANYTHING)
      }) orElse anonymousUser(request)
  }

  def anonymousUser(request: RequestHeader): Option[(ObjectId, PlayerOptions)] = {
    logger.trace(s"parse anonymous user")
    for {
      orgId <- orgIdFromCookie(request)
      options <- renderOptions(request)
    } yield {
      (new ObjectId(orgId), options)
    }
  }
}

