package org.corespring.v2.api

import org.corespring.platform.core.encryption.ApiClientEncryptionService
import org.corespring.v2.auth.encryption.CachingApiClientEncryptionService
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.auth.services.caching.CachingTokenService
import play.api.mvc.{ Action, AnyContent, Controller }

import scala.concurrent.{ ExecutionContext, Future }

trait Utils extends Controller {

  implicit def ec: ExecutionContext

  def tokenService: TokenService

  def apiClientEncryptionService: ApiClientEncryptionService

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
