package org.corespring.v2.auth.services

import org.corespring.platform.core.models.Organization
import org.corespring.v2.errors.V2Error
import scalaz.Validation
import play.api.mvc.RequestHeader

trait TokenService {

  def orgForToken(token: String)(implicit rh: RequestHeader): Validation[V2Error, Organization]
}
