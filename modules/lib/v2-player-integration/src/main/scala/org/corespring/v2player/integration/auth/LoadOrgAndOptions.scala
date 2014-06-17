package org.corespring.v2player.integration.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.controllers.auth.UserSession
import org.corespring.v2.auth.OrgTransformer
import org.corespring.v2player.integration.cookies.{ PlayerOptions, V2PlayerCookieReader }
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.Validation

trait LoadOrgAndOptions extends UserSession with V2PlayerCookieReader {

  protected lazy val logger = LoggerFactory.getLogger("v2Player.org-id-options")

  type OrgAndOpts = (ObjectId, PlayerOptions)
  type RequestToOrgAndOpts = RequestHeader => Option[(ObjectId, PlayerOptions)]

  def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String]

  def toOrgId(apiClientId: String): Option[ObjectId]

  def transformer: OrgTransformer[OrgAndOpts]

  /**
   * TODO: There seems like there is crossover her with <code>RequestTransformer</code>. Should look to centralize.
   * @param request
   * @return
   */
  def getOrgIdAndOptions(request: RequestHeader): Validation[String, OrgAndOpts] = transformer(request)

}

