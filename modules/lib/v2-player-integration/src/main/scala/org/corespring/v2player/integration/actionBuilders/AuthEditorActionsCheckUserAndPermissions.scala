package org.corespring.v2player.integration.actionBuilders

import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.v2player.integration.actionBuilders.access.Mode
import org.corespring.v2player.integration.controllers.editor.AuthEditorActions
import play.api.mvc.{ Action, SimpleResult, AnyContent, Request }
import scala.concurrent.Future
import scalaz.{ Failure, Success }
import org.corespring.platform.core.controllers.auth.SecureSocialService

abstract class AuthEditorActionsCheckPermissions(
  secureSocialService: SecureSocialService,
  userService: UserService,
  sessionService: MongoService,
  itemService: ItemService,
  orgService: OrganizationService)
  extends BaseAuth(secureSocialService, userService, sessionService, itemService, orgService)
  with AuthEditorActions {
  override def edit(itemId: String)(error: (Int, String) => Future[SimpleResult])(block: (Request[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
    request =>

      val result = checkAccess(
        request,
        orgCanAccessItem(itemId, _),
        hasPermissions(itemId, None, Mode.gather, _))

      result match {
        case Success(true) => block(request)
        case Success(false) => error(1001, "Access not granted")
        case Failure(err) => error(err.code, err.message)
      }
  }
}
