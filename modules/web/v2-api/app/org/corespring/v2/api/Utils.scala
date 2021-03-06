package org.corespring.v2.api

import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.services.auth.AccessTokenService
import org.corespring.v2.auth.encryption.CachingApiClientEncryptionService
import org.corespring.v2.auth.services.caching.CachingTokenService
import play.api.mvc.{ Action, AnyContent, Controller }

import scala.concurrent.{ ExecutionContext, Future }

class Utils(
  tokenService: AccessTokenService,
  apiClientEncryptionService: ApiClientEncryptionService,
  v2ApiContext: V2ApiExecutionContext) extends Controller {

  implicit def ec: ExecutionContext = v2ApiContext.context

  def flushCaches: Action[AnyContent] = Action.async { implicit request =>
    Future {

      if (tokenService.isInstanceOf[CachingTokenService]) {
        tokenService.asInstanceOf[CachingTokenService].flush
      }

      if (apiClientEncryptionService.isInstanceOf[CachingApiClientEncryptionService]) {
        apiClientEncryptionService.asInstanceOf[CachingApiClientEncryptionService].flush
      }

      Ok("done")
    }
  }
}
