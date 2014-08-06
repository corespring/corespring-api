package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ PlayerJs, PlayerLauncherHooks => ContainerPlayerLauncherHooks }
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.cookies.V2PlayerCookieWriter
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.compoundError
import org.corespring.v2.log.V2LoggerFactory
import play.api.mvc._

import scala.concurrent.Future
import scalaz._

trait PlayerLauncherHooks extends ContainerPlayerLauncherHooks with LoadOrgAndOptions {

  lazy val logger = V2LoggerFactory.getLogger("PlayerLauncherHooks")

  def secureSocialService: SecureSocialService

  def userService: UserService

  override def playerJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  override def editorJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  private def load(implicit header: RequestHeader): Future[PlayerJs] = Future {

    logger.trace(s"load js...")
    getOrgIdAndOptions(header) match {
      case Success(OrgAndOpts(orgId, opts, _)) => {
        PlayerJs(opts.secure, header.session)
      }
      case Failure(error) => error match {

        case compoundError(msg, errs, _) =>
          PlayerJs(false, header.session, Seq(s"${error.errorType}: errors: ${errs.map(e => s"${e.errorType}: ${e.message}").mkString("\n")}"))
        case _ =>
          PlayerJs(false, header.session, Seq(s"${error.errorType}: ${error.message}"))

      }
    }

  }

}
