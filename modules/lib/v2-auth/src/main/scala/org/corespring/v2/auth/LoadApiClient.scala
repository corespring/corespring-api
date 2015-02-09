package org.corespring.v2.auth

import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.errors.V2Error
import play.api.mvc.RequestHeader

import scalaz.Validation

trait LoadApiClient {
  def getApiClient(request: RequestHeader): Validation[V2Error, ApiClient]
}
