package org.corespring.v2.api

import org.corespring.platform.core.encryption.OrgEncryptionService
import org.corespring.v2.auth.encryption.CachingOrgEncryptionService
import org.corespring.v2.auth.services.TokenService
import org.corespring.v2.auth.services.caching.CachingTokenService
import play.api.mvc.{ Action, AnyContent, Controller }

import scala.concurrent.{ ExecutionContext, Future }

trait Utils extends Controller {

  implicit def ec: ExecutionContext

  def tokenService: TokenService

  def orgEncryptionService: OrgEncryptionService

  def flushCaches: Action[AnyContent] = Action.async { implicit request =>
    Future {

      if (tokenService.isInstanceOf[CachingTokenService]) {
        tokenService.asInstanceOf[CachingTokenService].flush
      }

      if (orgEncryptionService.isInstanceOf[CachingOrgEncryptionService]) {
        orgEncryptionService.asInstanceOf[CachingOrgEncryptionService].flush
      }

      Ok("done")
    }
  }
}
