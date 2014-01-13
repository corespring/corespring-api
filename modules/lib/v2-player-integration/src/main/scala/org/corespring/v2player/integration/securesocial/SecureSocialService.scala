package org.corespring.v2player.integration.securesocial

import play.api.mvc.{Request, AnyContent}
import securesocial.core.Identity

trait SecureSocialService {

  def currentUser(request:Request[AnyContent]) : Option[Identity]
}
