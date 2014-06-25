package org.corespring.v2.auth.services

import org.corespring.platform.core.models.Organization

trait TokenService {

  def orgForToken(token: String): Option[Organization]
}
