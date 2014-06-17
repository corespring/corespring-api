package org.corespring.v2player.integration.auth.wired

import org.corespring.v2player.integration.auth.{ AuthCheck, ItemAuth }
import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemAuthWired extends ItemAuth {

  def authCheck: AuthCheck

  override def canCreateItemInCollection(collectionId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    authCheck.orgCanWriteToCollection(collectionId).leftMap(_.message)
  }

  override def canWriteItem(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = {
    authCheck.hasAccess()
  }

  override def canAccessItem(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean] = ???
}
