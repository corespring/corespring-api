package org.corespring.platform.core.controllers.auth

import play.api.mvc.RequestHeader
import securesocial.core.Identity

trait SecureSocialService {

  def currentUser(request: RequestHeader): Option[Identity]
}
