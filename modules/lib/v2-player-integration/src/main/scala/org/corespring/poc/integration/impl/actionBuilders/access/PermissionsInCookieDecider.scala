package org.corespring.poc.integration.impl.actionBuilders.access

import play.api.mvc.{AnyContent, Request}
import org.corespring.player.accessControl.cookies.{BasePlayerCookieReader, PlayerCookieReader}
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.models.auth.Permission
import org.bson.types.ObjectId


trait V2PlayerCookieReader extends BasePlayerCookieReader[Mode.Mode, PlayerOptions]{
  def toMode(s: String): Mode.Mode = ???

  def toOptions(json: String): PlayerOptions = ???
}


/*class PermissionsInCookieDecider(itemService : ItemService, orgService : OrganizationService) extends AccessDecider with V2PlayerCookieReader{

  def accessForItemId(itemId: String, request: Request[AnyContent]): AccessResult = {

    //AccessResult(true, Seq.empty)

    for{
      orgId <- orgIdFromCookie(request)
      vid <- VersionedId(itemId)
      item <- itemService.findOneById(vid)
      canAccessCollection <- orgService.canAccessCollection(new ObjectId(orgId), new ObjectId(item.collectionId), Permission.Read)
    } yield canAccessCollection

  }

  def accessForSessionId(sessionId: String, request: Request[AnyContent]): AccessResult = ???
}*/
