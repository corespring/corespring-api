package org.corespring.web.user

import play.api.mvc.RequestHeader
import securesocial.core.Identity

trait SecureSocial {

  def currentUser(request: RequestHeader): Option[Identity] = securesocial.core.SecureSocial.currentUser(request)
}
