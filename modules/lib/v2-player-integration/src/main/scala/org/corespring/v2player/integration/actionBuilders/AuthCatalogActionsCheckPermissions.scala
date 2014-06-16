package org.corespring.v2player.integration.actionBuilders

import org.corespring.v2player.integration.actionBuilders.access.Mode
import org.corespring.v2player.integration.controllers.catalog.AuthCatalogActions
import play.api.mvc.{ Action, AnyContent, Request, SimpleResult }

import scala.concurrent.Future
import scalaz.{ Failure, Success }

abstract class AuthCatalogActionsCheckPermissions(
  auth: AuthCheck)
  extends AuthCatalogActions {
  override def show(itemId: String)(error: (Int, String) => Future[SimpleResult])(block: (Request[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async {
    request =>

      val result = auth.hasAccess(
        request,
        auth.orgCanAccessItem(itemId, _),
        auth.hasPermissions(itemId, None, Mode.gather, _))

      result match {
        case Success(true) => block(request)
        case Success(false) => error(1001, "Access not granted")
        case Failure(err) => error(err.code, err.message)
      }
  }
}
