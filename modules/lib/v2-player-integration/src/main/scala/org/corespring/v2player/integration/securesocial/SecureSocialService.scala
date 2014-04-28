package org.corespring.v2player.integration.securesocial

import play.api.mvc.{ RequestHeader, Request, AnyContent }
import securesocial.core.Identity

trait SecureSocialService {

  def currentUser(request: RequestHeader): Option[Identity]
}
