package org.corespring.v2.auth.identifiers

import org.corespring.platform.core.models.auth.{ApiClientService, ApiClient}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc.RequestHeader

import scalaz.Validation

trait ApiClientRequestIdentity[B] extends RequestIdentity[B] {
  def apiClientService: ApiClientService

  def headerToApiClient(rh: RequestHeader): Validation[V2Error, ApiClient]

  def data(rh: RequestHeader, apiClient: ApiClient): B

  lazy val logger = V2LoggerFactory.getLogger("auth", "ApiClientRequestIdentity")

  def apply(rh: RequestHeader): Validation[V2Error, B] = {
    logger.trace(s"apply: ${rh.path}")

    for {
      apiClient <- headerToApiClient(rh)
    } yield {
      data(rh, apiClient)
    }
  }

}
