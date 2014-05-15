package org.corespring.api.v2.services

import org.corespring.platform.core.models.Organization

trait TokenService {

  def orgForToken(token: String): Option[Organization]
}
